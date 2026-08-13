package com.jp.whatsappclone.data.repository

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageException
import com.jp.whatsappclone.data.domain.MessageKind
import com.jp.whatsappclone.data.domain.Message
import com.jp.whatsappclone.data.domain.MessageDeliveryStatus
import com.jp.whatsappclone.data.domain.MessagePageRequest
import com.jp.whatsappclone.data.domain.MessagePageResult
import com.jp.whatsappclone.data.domain.Reaction
import com.jp.whatsappclone.data.domain.Receipt
import com.jp.whatsappclone.data.domain.SendTextCommand
import com.jp.whatsappclone.data.domain.SendAttachmentCommand
import com.jp.whatsappclone.data.local.ConversationStateDao
import com.jp.whatsappclone.data.local.ConversationStateEntity
import com.jp.whatsappclone.data.local.DraftDao
import com.jp.whatsappclone.data.local.DraftEntity
import com.jp.whatsappclone.data.local.InnovfixDatabase
import com.jp.whatsappclone.data.local.InboxDao
import com.jp.whatsappclone.data.local.MessageDao
import com.jp.whatsappclone.data.local.MessageEntity
import com.jp.whatsappclone.data.local.OutboxDao
import com.jp.whatsappclone.data.local.OutboxEntity
import com.jp.whatsappclone.data.local.OutboxOperation
import com.jp.whatsappclone.data.local.OutboxState
import com.jp.whatsappclone.data.local.ReactionDao
import com.jp.whatsappclone.data.local.ReactionEntity
import com.jp.whatsappclone.data.local.ReceiptDao
import com.jp.whatsappclone.data.local.ReceiptEntity
import com.jp.whatsappclone.data.local.SearchDao
import com.jp.whatsappclone.data.local.SearchIndex
import com.jp.whatsappclone.data.mapper.toDomain
import com.jp.whatsappclone.data.mapper.toMessageEntity
import com.jp.whatsappclone.data.mapper.toReactionEntity
import com.jp.whatsappclone.data.mapper.toReceiptEntity
import com.jp.whatsappclone.data.remote.FirebaseServices
import com.jp.whatsappclone.data.remote.BackendConfiguration
import com.jp.whatsappclone.data.remote.TypingDocumentDto
import com.jp.whatsappclone.data.sync.OutboxDrainResult
import com.jp.whatsappclone.data.sync.OutboxProcessor
import com.jp.whatsappclone.data.sync.OutboxScheduler
import com.jp.whatsappclone.data.util.UuidV7
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class FirebaseMessagingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val services: FirebaseServices,
    private val configuration: BackendConfiguration,
    private val database: InnovfixDatabase,
    private val messageDao: MessageDao,
    private val inboxDao: InboxDao,
    private val reactionDao: ReactionDao,
    private val receiptDao: ReceiptDao,
    private val outboxDao: OutboxDao,
    private val draftDao: DraftDao,
    private val conversationStateDao: ConversationStateDao,
    private val searchDao: SearchDao,
    private val outboxScheduler: OutboxScheduler,
) : MessagingRepository, OutboxProcessor {
    private val drainMutex = kotlinx.coroutines.sync.Mutex()

    init {
        outboxScheduler.attach(this)
    }

    override fun observeMessages(conversationId: String): Flow<PagingData<Message>> =
        Pager(
            PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                prefetchDistance = PAGE_PREFETCH_DISTANCE,
                maxSize = PAGE_SIZE * MAX_CACHED_PAGES,
                enablePlaceholders = false,
            ),
        ) {
            messageDao.pagingSource(conversationId)
        }.flow.map { page -> page.map { it.toDomain() } }

    override fun observeCachedMessages(conversationId: String): Flow<List<Message>> = channelFlow {
        val listenerOwnerId = services.auth?.currentUser?.uid
        launch {
            messageDao.observeRecent(conversationId).collect { entities ->
                // The list is deliberately bounded. It keeps the latest-50 Firestore listener
                // alive and exposes its Room reconciliation without materializing history.
                send(entities.asReversed().map { it.toDomain() })
            }
        }
        val listener = listenToRecentMessages(conversationId) { committed ->
            if (listenerOwnerId != null) {
                launch { reconcileRecentMessages(conversationId, committed, listenerOwnerId) }
            }
        }
        awaitClose { listener?.remove() }
    }

    override fun observeReactions(conversationId: String): Flow<List<Reaction>> = channelFlow {
        val listenerOwnerId = services.auth?.currentUser?.uid
        launch {
            reactionDao.observe(conversationId).collect { entities ->
                send(entities.map { it.toDomain() })
            }
        }
        val listener: ListenerRegistration? = services.firestore
            ?.collection("conversations")
            ?.document(conversationId)
            ?.collection("reactions")
            ?.addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.metadata.isFromCache && listenerOwnerId != null) {
                    val reactions = snapshot.documents.mapNotNull { it.toReactionEntity(conversationId) }
                    launch {
                        database.withTransaction {
                            if (hasLocalAccess(listenerOwnerId, conversationId)) {
                                val merged = reactions.toMutableList()
                                // A server snapshot can arrive before this device's queued write.
                                // Reapply the Room outbox overlay so optimistic UI does not flicker
                                // or silently revert while offline/retrying.
                                outboxDao.reactionOperations(conversationId).forEach { operation ->
                                    val messageId = operation.messageId ?: return@forEach
                                    merged.removeAll {
                                        it.messageId == messageId && it.userId == listenerOwnerId
                                    }
                                    operation.reaction?.let { emoji ->
                                        merged += ReactionEntity(
                                            conversationId = conversationId,
                                            messageId = messageId,
                                            userId = listenerOwnerId,
                                            emoji = emoji,
                                            createdAtMillis = operation.clientCreatedAtMillis,
                                            updatedAtMillis = operation.clientCreatedAtMillis,
                                        )
                                    }
                                }
                                reactionDao.replaceForConversation(conversationId, merged)
                            }
                        }
                    }
                }
            }
        awaitClose { listener?.remove() }
    }

    override fun observeReceipts(conversationId: String): Flow<List<Receipt>> = channelFlow {
        val listenerOwnerId = services.auth?.currentUser?.uid
        launch {
            receiptDao.observe(conversationId).collect { entities ->
                send(entities.map { it.toDomain() })
            }
        }
        val listener: ListenerRegistration? = services.firestore
            ?.collection("conversations")
            ?.document(conversationId)
            ?.collection("members")
            ?.addSnapshotListener { snapshot, _ ->
                val receipts = snapshot?.documents.orEmpty().mapNotNull {
                    it.toReceiptEntity(conversationId)
                }
                if (snapshot != null && !snapshot.metadata.isFromCache && listenerOwnerId != null) launch {
                    database.withTransaction {
                        if (hasLocalAccess(listenerOwnerId, conversationId)) {
                            receiptDao.replaceForConversation(conversationId, receipts)
                        }
                    }
                }
            }
        awaitClose { listener?.remove() }
    }

    override suspend fun pageMessages(request: MessagePageRequest): Result<MessagePageResult> = runCatching {
        require(request.limit in 1..100) { "Page limit must be between 1 and 100" }
        val ownerId = requireUserId()
        requireLocalAccess(ownerId, request.conversationId)
        val firestore = services.firestore ?: return@runCatching MessagePageResult(0, false)
        val storedState = conversationStateDao.get(request.conversationId)
        if (request.beforeMessageId == null && storedState?.hasMoreHistory == false) {
            return@runCatching MessagePageResult(0, false)
        }
        var query: Query = firestore.collection("conversations")
            .document(request.conversationId)
            .collection("messages")
            .orderBy("serverCreatedAt", Query.Direction.DESCENDING)
            .limit(request.limit.toLong())
        val resolvedCursorId = request.beforeMessageId
            ?: storedState?.oldestLoadedMessageId
        if (resolvedCursorId != null) {
            val cursor = firestore.collection("conversations")
                .document(request.conversationId)
                .collection("messages")
                .document(resolvedCursorId)
                .get()
                .await()
            requireLocalAccess(ownerId, request.conversationId)
            if (!cursor.exists()) {
                database.withTransaction {
                    check(hasLocalAccess(ownerId, request.conversationId)) { "Conversation access changed" }
                    val previousState = conversationStateDao.get(request.conversationId)
                    conversationStateDao.upsert(
                        ConversationStateEntity(
                            conversationId = request.conversationId,
                            oldestLoadedMessageId = previousState?.oldestLoadedMessageId,
                            hasMoreHistory = false,
                            lastSnapshotAtMillis = System.currentTimeMillis(),
                            lastReceiptWriteAtMillis = previousState?.lastReceiptWriteAtMillis,
                        ),
                    )
                }
                return@runCatching MessagePageResult(0, false)
            }
            query = query.startAfter(cursor)
        }
        val snapshot = query.get().await()
        requireLocalAccess(ownerId, request.conversationId)
        val messages = snapshot.documents.mapNotNull { it.toMessageEntity(request.conversationId) }
        database.withTransaction {
            check(hasLocalAccess(ownerId, request.conversationId)) { "Conversation access changed" }
            if (messages.isNotEmpty()) {
                val mergedMessages = preserveLocalMediaPaths(messages)
                messageDao.upsert(mergedMessages)
                updateMessageSearchIndex(mergedMessages)
            }
            val previousState = conversationStateDao.get(request.conversationId)
            conversationStateDao.upsert(
                ConversationStateEntity(
                    conversationId = request.conversationId,
                    oldestLoadedMessageId = messages.lastOrNull()?.messageId
                        ?: previousState?.oldestLoadedMessageId,
                    hasMoreHistory = messages.size >= request.limit,
                    lastSnapshotAtMillis = System.currentTimeMillis(),
                    lastReceiptWriteAtMillis = previousState?.lastReceiptWriteAtMillis,
                ),
            )
        }
        MessagePageResult(messages.size, messages.size >= request.limit)
    }

    override suspend fun sendText(command: SendTextCommand): Result<String> = runCatching {
        val senderId = requireUserId()
        val body = command.body.trim()
        require(body.isNotEmpty()) { "Message cannot be empty" }
        require(body.length <= MAX_TEXT_LENGTH) { "Message cannot exceed $MAX_TEXT_LENGTH characters" }
        requireUuidV7(command.messageId)
        command.replyToMessageId?.let(::requireUuidV7)
        val message = MessageEntity(
            messageId = command.messageId,
            conversationId = command.conversationId,
            senderId = senderId,
            body = body,
            clientCreatedAtMillis = command.clientCreatedAtMillis,
            replyToMessageId = command.replyToMessageId,
            localStatus = MessageDeliveryStatus.PENDING.name,
        )
        val operation = OutboxEntity(
            operationId = "send:${command.messageId}",
            operationType = OutboxOperation.SEND_MESSAGE,
            conversationId = command.conversationId,
            messageId = command.messageId,
            body = body,
            replyToMessageId = command.replyToMessageId,
            clientCreatedAtMillis = command.clientCreatedAtMillis,
        )
        database.withTransaction {
            check(hasLocalAccess(senderId, command.conversationId)) { "Conversation is not available for this account" }
            val existing = messageDao.get(command.messageId)
            if (existing == null) {
                messageDao.insertIfAbsent(message)
                searchDao.index(SearchIndex.message(message))
                outboxDao.upsert(operation)
                val inbox = inboxDao.get(senderId, command.conversationId)
                if (inbox != null) {
                    val updatedInbox = inbox.copy(
                            lastMessageId = command.messageId,
                            lastMessagePreview = body,
                            lastMessageSenderId = senderId,
                            lastActivityAtMillis = command.clientCreatedAtMillis,
                            updatedAtMillis = command.clientCreatedAtMillis,
                        )
                    inboxDao.upsert(updatedInbox)
                    searchDao.index(SearchIndex.inbox(updatedInbox))
                }
            } else if (existing.conversationId != command.conversationId || existing.body != body) {
                error("Message ID already belongs to different content")
            }
        }
        outboxScheduler.schedule()
        command.messageId
    }

    override suspend fun sendAttachment(command: SendAttachmentCommand): Result<String> = runCatching {
        val senderId = requireUserId()
        require(command.kind != MessageKind.TEXT) { "Attachment type is required" }
        requireUuidV7(command.messageId)
        command.replyToMessageId?.let(::requireUuidV7)
        requireLocalAccess(senderId, command.conversationId)
        check(messageDao.get(command.messageId) == null) { "Attachment was already sent" }
        val safeName = sanitizeFileName(command.fileName, command.kind)
        val storagePath = "organizations/${configuration.organizationId}/conversations/" +
            "${command.conversationId}/messages/${command.messageId}/$safeName"
        val localFile = persistOutgoingAttachment(command, safeName)
        val localMediaPath = Uri.fromFile(localFile).toString()
        val actualSizeBytes = localFile.length()

        val body = command.caption?.trim()?.takeIf { it.isNotEmpty() }
            ?: attachmentPreview(command.kind, safeName)
        val message = MessageEntity(
            messageId = command.messageId,
            conversationId = command.conversationId,
            senderId = senderId,
            body = body,
            clientCreatedAtMillis = command.clientCreatedAtMillis,
            replyToMessageId = command.replyToMessageId,
            localStatus = MessageDeliveryStatus.PENDING.name,
            kind = command.kind.name,
            storagePath = storagePath,
            fileName = safeName,
            mimeType = command.mimeType,
            sizeBytes = actualSizeBytes,
            durationMillis = command.durationMillis,
            localMediaPath = localMediaPath,
        )
        val operation = OutboxEntity(
            operationId = "send:${command.messageId}",
            operationType = OutboxOperation.SEND_MESSAGE,
            conversationId = command.conversationId,
            messageId = command.messageId,
            body = body,
            kind = command.kind.name,
            storagePath = storagePath,
            fileName = safeName,
            mimeType = command.mimeType,
            sizeBytes = actualSizeBytes,
            durationMillis = command.durationMillis,
            localMediaPath = localMediaPath,
            replyToMessageId = command.replyToMessageId,
            clientCreatedAtMillis = command.clientCreatedAtMillis,
        )
        database.withTransaction {
            check(hasLocalAccess(senderId, command.conversationId)) { "Conversation is not available for this account" }
            check(messageDao.get(command.messageId) == null) { "Attachment was already sent" }
            messageDao.insertIfAbsent(message)
            searchDao.index(SearchIndex.message(message))
            outboxDao.upsert(operation)
            inboxDao.get(senderId, command.conversationId)?.let { inbox ->
                val updated = inbox.copy(
                    lastMessageId = command.messageId,
                    lastMessagePreview = body,
                    lastMessageSenderId = senderId,
                    lastActivityAtMillis = command.clientCreatedAtMillis,
                    updatedAtMillis = command.clientCreatedAtMillis,
                )
                inboxDao.upsert(updated)
                searchDao.index(SearchIndex.inbox(updated))
            }
        }
        outboxScheduler.schedule()
        command.messageId
    }

    override suspend fun forwardMessage(
        sourceConversationId: String,
        messageId: String,
        targetConversationId: String,
    ): Result<String> = runCatching {
        val userId = requireUserId()
        requireLocalAccess(userId, sourceConversationId)
        requireLocalAccess(userId, targetConversationId)
        val source = messageDao.get(messageId) ?: error("Message is not downloaded")
        require(source.conversationId == sourceConversationId) { "Message belongs to another conversation" }
        require(!source.deleted) { "Deleted messages cannot be forwarded" }
        val kind = runCatching { MessageKind.valueOf(source.kind) }.getOrDefault(MessageKind.TEXT)
        if (kind == MessageKind.TEXT) {
            messagingSendText(targetConversationId, source.body)
        } else {
            val localPath = prepareAttachment(messageId).getOrThrow()
            sendAttachment(
                SendAttachmentCommand(
                    conversationId = targetConversationId,
                    sourceUri = localPath,
                    kind = kind,
                    fileName = source.fileName ?: attachmentDefaultName(kind),
                    mimeType = source.mimeType.orEmpty(),
                    sizeBytes = source.sizeBytes,
                    durationMillis = source.durationMillis,
                ),
            ).getOrThrow()
        }
    }

    override suspend fun prepareAttachment(messageId: String): Result<String> = runCatching {
        val userId = requireUserId()
        val message = messageDao.get(messageId) ?: error("Message is not downloaded")
        requireLocalAccess(userId, message.conversationId)
        message.localMediaPath?.let { local ->
            val uri = Uri.parse(local)
            val usable = if (uri.scheme == "file") File(requireNotNull(uri.path)).exists()
            else runCatching { context.contentResolver.openInputStream(uri)?.use { true } ?: false }.getOrDefault(false)
            if (usable) return@runCatching local
        }
        val storagePath = message.storagePath ?: error("Attachment path is missing")
        val suffix = message.fileName?.substringAfterLast('.', "bin")?.take(8) ?: "bin"
        val output = File(context.cacheDir, "message_${message.messageId}.$suffix")
        services.storage?.reference?.child(storagePath)?.getFile(output)?.await()
            ?: error("Firebase Storage is unavailable")
        val uri = Uri.fromFile(output).toString()
        messageDao.setLocalMediaPath(messageId, uri)
        uri
    }

    private suspend fun messagingSendText(conversationId: String, body: String): String =
        sendText(SendTextCommand(conversationId = conversationId, body = body)).getOrThrow()

    override suspend fun retry(conversationId: String, messageId: String): Result<Unit> = runCatching {
        val ownerId = requireUserId()
        val message = messageDao.get(messageId) ?: error("Message not found")
        require(message.conversationId == conversationId) { "Message belongs to another conversation" }
        require(message.senderId == ownerId) { "Only the sender can retry this message" }
        database.withTransaction {
            check(hasLocalAccess(ownerId, conversationId)) { "Conversation is not available for this account" }
            outboxDao.upsert(
                OutboxEntity(
                    operationId = "send:$messageId",
                    operationType = OutboxOperation.SEND_MESSAGE,
                    conversationId = conversationId,
                    messageId = messageId,
                    body = message.body,
                    kind = message.kind,
                    storagePath = message.storagePath,
                    fileName = message.fileName,
                    mimeType = message.mimeType,
                    sizeBytes = message.sizeBytes,
                    durationMillis = message.durationMillis,
                    localMediaPath = message.localMediaPath,
                    replyToMessageId = message.replyToMessageId,
                    clientCreatedAtMillis = message.clientCreatedAtMillis,
                ),
            )
            messageDao.updateStatus(messageId, MessageDeliveryStatus.PENDING.name)
        }
        outboxScheduler.schedule()
    }

    override suspend fun setReaction(
        conversationId: String,
        messageId: String,
        reaction: String?,
    ): Result<Unit> = runCatching {
        val userId = requireUserId()
        val message = messageDao.get(messageId) ?: error("Message not found")
        require(message.conversationId == conversationId) { "Message belongs to another conversation" }
        if (reaction != null) require(reaction in SUPPORTED_REACTIONS) { "Unsupported reaction" }
        val now = System.currentTimeMillis()
        database.withTransaction {
            check(hasLocalAccess(userId, conversationId)) { "Conversation is not available for this account" }
            if (reaction == null) reactionDao.delete(messageId, userId)
            else reactionDao.upsert(
                ReactionEntity(conversationId, messageId, userId, reaction, now, now),
            )
            outboxDao.upsert(
                OutboxEntity(
                    operationId = "reaction:$messageId:$userId",
                    operationType = OutboxOperation.SET_REACTION,
                    conversationId = conversationId,
                    messageId = messageId,
                    reaction = reaction,
                    clientCreatedAtMillis = now,
                ),
            )
        }
        outboxScheduler.schedule()
    }

    override suspend fun deleteForEveryone(conversationId: String, messageId: String): Result<Unit> = runCatching {
        val message = messageDao.get(messageId) ?: error("Message not found")
        require(message.conversationId == conversationId) { "Message belongs to another conversation" }
        val userId = requireUserId()
        require(message.senderId == userId) { "Only the sender can delete this message" }
        val now = System.currentTimeMillis()
        database.withTransaction {
            check(hasLocalAccess(userId, conversationId)) { "Conversation is not available for this account" }
            outboxDao.upsert(
                OutboxEntity(
                    operationId = "delete:$messageId",
                    operationType = OutboxOperation.DELETE_MESSAGE,
                    conversationId = conversationId,
                    messageId = messageId,
                    clientCreatedAtMillis = now,
                ),
            )
        }
        outboxScheduler.schedule()
    }

    override suspend fun markRead(conversationId: String, throughMessageId: String): Result<Unit> = runCatching {
        val userId = requireUserId()
        val message = messageDao.get(throughMessageId) ?: error("Message not found")
        require(message.conversationId == conversationId) { "Message belongs to another conversation" }
        val watermark = message.serverCreatedAtMillis ?: message.clientCreatedAtMillis
        val now = System.currentTimeMillis()
        database.withTransaction {
            check(hasLocalAccess(userId, conversationId)) { "Conversation is not available for this account" }
            val existing = receiptDao.get(conversationId, userId)
            val delivered = maxOf(existing?.lastDeliveredAtMillis ?: 0L, watermark)
            val read = maxOf(existing?.lastReadAtMillis ?: 0L, watermark)
            receiptDao.upsert(
                listOf(ReceiptEntity(conversationId, userId, delivered, read.coerceAtMost(delivered), now)),
            )
            outboxDao.upsert(
                OutboxEntity(
                    operationId = "read:$conversationId",
                    operationType = OutboxOperation.MARK_READ,
                    conversationId = conversationId,
                    watermarkMessageId = throughMessageId,
                    clientCreatedAtMillis = now,
                ),
            )
            inboxDao.markRead(userId, conversationId, now)
        }
        outboxScheduler.schedule()
    }

    override suspend fun setTyping(conversationId: String, isTyping: Boolean): Result<Unit> = runCatching {
        val userId = requireUserId()
        requireLocalAccess(userId, conversationId)
        val firestore = services.firestore ?: return@runCatching
        val reference = firestore.collection("conversations")
            .document(conversationId)
            .collection("typing")
            .document(userId)
        if (!isTyping) {
            reference.delete().await()
        } else {
            reference.set(
                mapOf(
                    "userId" to userId,
                    "isTyping" to true,
                    "expiresAt" to Timestamp(Date(System.currentTimeMillis() + TYPING_TTL_MILLIS)),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
        requireLocalAccess(userId, conversationId)
    }

    override fun observeTyping(conversationId: String): Flow<Set<String>> = channelFlow {
        val values = mutableMapOf<String, Long>()
        val listener: ListenerRegistration? = services.firestore
            ?.collection("conversations")
            ?.document(conversationId)
            ?.collection("typing")
            ?.addSnapshotListener { snapshot, _ ->
                synchronized(values) {
                    values.clear()
                    snapshot?.documents.orEmpty().forEach { document ->
                        document.toObject(TypingDocumentDto::class.java)?.let { typing ->
                            if (typing.isTyping) values[typing.userId] = typing.expiresAt?.toDate()?.time ?: 0
                        }
                    }
                }
            }
        launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val active = synchronized(values) {
                    values.filterValues { it > now }.keys.toSet()
                }
                send(active)
                delay(1_000)
            }
        }
        awaitClose { listener?.remove() }
    }.distinctUntilChanged()

    override fun observeDraft(conversationId: String): Flow<String> =
        draftDao.observeBody(conversationId).map { it.orEmpty() }

    override suspend fun saveDraft(conversationId: String, body: String) {
        val ownerId = services.auth?.currentUser?.uid ?: return
        database.withTransaction {
            if (!hasLocalAccess(ownerId, conversationId)) return@withTransaction
            if (body.isEmpty()) draftDao.delete(conversationId)
            else draftDao.upsert(DraftEntity(conversationId, ownerId, body, System.currentTimeMillis()))
        }
    }

    override suspend fun drain(limit: Int): OutboxDrainResult = drainMutex.withLock {
        val ownerId = services.auth?.currentUser?.uid
        if (
            services.firestore == null ||
            ownerId == null ||
            !database.isOwnedBy(ownerId, configuration.organizationId)
        ) {
            return@withLock OutboxDrainResult(0, 0, false)
        }
        val recovered = database.withTransaction {
            if (!isLocalOwner(ownerId)) return@withTransaction false
            outboxDao.recoverInterruptedForOwner(ownerId, configuration.organizationId)
            true
        }
        if (!recovered) return@withLock OutboxDrainResult(0, 0, false)
        val ready = outboxDao.readyForOwner(
            ownerId = ownerId,
            orgId = configuration.organizationId,
            nowMillis = System.currentTimeMillis(),
            limit = limit,
        )
        var processed = 0
        var failed = 0
        for (operation in ready) {
            if (!isCurrentSession(ownerId) || !database.isOwnedBy(ownerId, configuration.organizationId)) {
                return@withLock OutboxDrainResult(processed, failed, false)
            }
            val claimed = database.withTransaction {
                if (!hasLocalAccess(ownerId, operation.conversationId)) return@withTransaction false
                outboxDao.setState(
                    operation.operationId,
                    operation.clientCreatedAtMillis,
                    OutboxState.PROCESSING,
                ) == 1
            }
            if (!claimed) continue
            try {
                execute(operation, ownerId)
                if (!isCurrentSession(ownerId) || !database.isOwnedBy(ownerId, configuration.organizationId)) {
                    return@withLock OutboxDrainResult(processed, failed, false)
                }
                database.withTransaction {
                    check(hasLocalAccess(ownerId, operation.conversationId)) { "Conversation access changed" }
                    outboxDao.delete(operation.operationId, operation.clientCreatedAtMillis)
                    operation.messageId?.takeIf { operation.operationType == OutboxOperation.SEND_MESSAGE }
                        ?.let { messageDao.updateStatus(it, MessageDeliveryStatus.SENT.name) }
                    operation.messageId?.takeIf { operation.operationType == OutboxOperation.DELETE_MESSAGE }
                        ?.let { messageId ->
                            messageDao.markDeleted(messageId, System.currentTimeMillis())
                            searchDao.remove(SearchIndex.MESSAGE, messageId)
                        }
                }
                processed++
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                if (!isCurrentSession(ownerId) || !database.isOwnedBy(ownerId, configuration.organizationId)) {
                    return@withLock OutboxDrainResult(processed, failed, false)
                }
                val exponent = min(operation.attemptCount, 10)
                val nextAttempt = System.currentTimeMillis() + min(10_000L * (1L shl exponent), 3_600_000L)
                database.withTransaction {
                    check(hasLocalAccess(ownerId, operation.conversationId)) { "Conversation access changed" }
                    outboxDao.markFailed(
                        operation.operationId,
                        operation.clientCreatedAtMillis,
                        failure::class.java.simpleName,
                        nextAttempt,
                    )
                    operation.messageId?.takeIf { operation.operationType == OutboxOperation.SEND_MESSAGE }
                        ?.let { messageDao.updateStatus(it, MessageDeliveryStatus.FAILED.name, "Send failed") }
                }
                failed++
            }
        }
        val nextAttemptAtMillis = if (isLocalOwner(ownerId)) {
            outboxDao.earliestOutstandingAtMillisForOwner(ownerId, configuration.organizationId)
        } else {
            null
        }
        OutboxDrainResult(
            processedCount = processed,
            failedCount = failed,
            hasMore = nextAttemptAtMillis != null,
            nextAttemptAtMillis = nextAttemptAtMillis,
        )
    }

    private suspend fun execute(operation: OutboxEntity, ownerId: String) {
        when (operation.operationType) {
            OutboxOperation.SEND_MESSAGE -> uploadMessage(operation, ownerId)
            OutboxOperation.SET_REACTION -> uploadReaction(operation, ownerId)
            OutboxOperation.DELETE_MESSAGE -> uploadDeletion(operation, ownerId)
            OutboxOperation.MARK_DELIVERED -> uploadDeliveryReceipt(operation, ownerId)
            OutboxOperation.MARK_READ -> uploadReadReceipt(operation, ownerId)
            OutboxOperation.SET_TYPING -> Unit
        }
    }

    private suspend fun uploadMessage(operation: OutboxEntity, senderId: String) {
        requireLocalAccess(senderId, operation.conversationId)
        val firestore = services.firestore ?: error("Firebase unavailable")
        val messageId = operation.messageId ?: error("Missing message ID")
        requireUuidV7(messageId)
        if (operation.kind != MessageKind.TEXT.name) ensureAttachmentUploaded(operation)
        val reference = firestore.collection("conversations")
            .document(operation.conversationId)
            .collection("messages")
            .document(messageId)
        val document = mutableMapOf<String, Any?>(
            "senderId" to senderId,
            "text" to requireNotNull(operation.body),
            "clientCreatedAt" to Timestamp(Date(operation.clientCreatedAtMillis)),
            "serverCreatedAt" to FieldValue.serverTimestamp(),
            "replyToMessageId" to operation.replyToMessageId,
            "deletedAt" to null,
            "schemaVersion" to 1,
            "kind" to operation.kind,
        )
        if (operation.kind != MessageKind.TEXT.name) {
            document["storagePath"] = operation.storagePath
            document["fileName"] = operation.fileName
            document["mimeType"] = operation.mimeType
            document["sizeBytes"] = operation.sizeBytes
            document["durationMillis"] = operation.durationMillis
        }
        firestore.runTransaction { transaction ->
            val existing = transaction.get(reference)
            if (!existing.exists()) {
                transaction.set(
                    reference,
                    document,
                )
            } else {
                require(existing.getString("senderId") == senderId) { "Remote message sender mismatch" }
                require(existing.getString("text") == operation.body) { "Remote message body mismatch" }
                require(existing.getString("replyToMessageId") == operation.replyToMessageId) {
                    "Remote reply target mismatch"
                }
                require(existing.getString("kind") == operation.kind) { "Remote message type mismatch" }
            }
        }.await()
        requireLocalAccess(senderId, operation.conversationId)
    }

    /** Uploads a media payload exactly once while keeping SEND_MESSAGE retries idempotent. */
    private suspend fun ensureAttachmentUploaded(operation: OutboxEntity) {
        val storagePath = operation.storagePath ?: error("Attachment path is missing")
        val localMediaPath = operation.localMediaPath ?: error("Local attachment copy is missing")
        val storage = services.storage ?: error("Firebase Storage is unavailable")
        val reference = storage.reference.child(storagePath)
        val remoteMetadata = try {
            reference.metadata.await()
        } catch (failure: StorageException) {
            if (failure.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) null else throw failure
        }
        if (remoteMetadata != null) {
            operation.sizeBytes?.let { expected ->
                check(remoteMetadata.sizeBytes == expected) { "Remote attachment size mismatch" }
            }
            return
        }

        val source = Uri.parse(localMediaPath)
        val localFile = source.path?.let(::File)
        check(source.scheme == "file" && localFile?.isFile == true && localFile.length() > 0L) {
            "Local attachment is no longer available"
        }
        val metadata = StorageMetadata.Builder().apply {
            operation.mimeType?.takeIf(String::isNotBlank)?.let { contentType = it }
        }.build()
        reference.putFile(source, metadata).await()
    }

    private suspend fun uploadReaction(operation: OutboxEntity, userId: String) {
        requireLocalAccess(userId, operation.conversationId)
        val firestore = services.firestore ?: error("Firebase unavailable")
        val messageId = operation.messageId ?: error("Missing message ID")
        val reference = firestore.collection("conversations")
            .document(operation.conversationId)
            .collection("reactions")
            .document("${messageId}_$userId")
        val reaction = operation.reaction
        firestore.runTransaction { transaction ->
            val existing = transaction.get(reference)
            if (reaction == null) {
                // A retried removal is already complete when the document no longer exists.
                if (existing.exists()) {
                    require(existing.getString("userId") == userId) { "Remote reaction owner mismatch" }
                    require(existing.getString("messageId") == messageId) { "Remote reaction message mismatch" }
                    transaction.delete(reference)
                }
            } else {
                require(reaction in SUPPORTED_REACTIONS)
                if (existing.exists()) {
                    require(existing.getString("userId") == userId) { "Remote reaction owner mismatch" }
                    require(existing.getString("messageId") == messageId) { "Remote reaction message mismatch" }
                    transaction.update(
                        reference,
                        mapOf("emoji" to reaction, "updatedAt" to FieldValue.serverTimestamp()),
                    )
                } else {
                    transaction.set(
                        reference,
                        mapOf(
                            "messageId" to messageId,
                            "userId" to userId,
                            "emoji" to reaction,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
            }
        }.await()
        requireLocalAccess(userId, operation.conversationId)
    }

    private suspend fun uploadDeletion(operation: OutboxEntity, ownerId: String) {
        requireLocalAccess(ownerId, operation.conversationId)
        val firestore = services.firestore ?: error("Firebase unavailable")
        val messageId = operation.messageId ?: error("Missing message ID")
        val reference = firestore.collection("conversations")
            .document(operation.conversationId)
            .collection("messages")
            .document(messageId)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(reference)
            require(existing.exists()) { "Remote message is unavailable" }
            require(existing.getString("senderId") == ownerId) { "Only the sender can delete this message" }
            if (existing.getTimestamp("deletedAt") == null) {
                transaction.update(reference, mapOf("text" to "", "deletedAt" to FieldValue.serverTimestamp()))
            } else {
                // A crash after the original commit may retry this operation. The tombstone is success.
                require(existing.getString("text").orEmpty().isEmpty()) { "Remote tombstone is invalid" }
            }
        }.await()
        requireLocalAccess(ownerId, operation.conversationId)
    }

    private suspend fun uploadReadReceipt(operation: OutboxEntity, userId: String) {
        requireLocalAccess(userId, operation.conversationId)
        val firestore = services.firestore ?: error("Firebase unavailable")
        val messageId = operation.watermarkMessageId ?: error("Missing receipt watermark")
        val message = messageDao.get(messageId) ?: error("Receipt message is not cached")
        val watermark = Timestamp(Date(message.serverCreatedAtMillis ?: message.clientCreatedAtMillis))
        val reference = firestore.collection("conversations")
            .document(operation.conversationId)
            .collection("members")
            .document(userId)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(reference)
            val currentDelivered = existing.getTimestamp("lastDeliveredAt") ?: Timestamp(0, 0)
            val currentRead = existing.getTimestamp("lastReadAt") ?: Timestamp(0, 0)
            val nextDelivered = maxTimestamp(currentDelivered, watermark)
            val nextRead = maxTimestamp(currentRead, watermark)
            transaction.update(
                reference,
                mapOf(
                    "lastDeliveredAt" to maxTimestamp(nextDelivered, nextRead),
                    "lastReadAt" to nextRead,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }.await()
        requireLocalAccess(userId, operation.conversationId)
    }

    private suspend fun uploadDeliveryReceipt(operation: OutboxEntity, userId: String) {
        requireLocalAccess(userId, operation.conversationId)
        val firestore = services.firestore ?: error("Firebase unavailable")
        val messageId = operation.watermarkMessageId ?: error("Missing receipt watermark")
        val message = messageDao.get(messageId) ?: error("Receipt message is not cached")
        val watermark = Timestamp(Date(message.serverCreatedAtMillis ?: message.clientCreatedAtMillis))
        val reference = firestore.collection("conversations")
            .document(operation.conversationId)
            .collection("members")
            .document(userId)
        firestore.runTransaction { transaction ->
            val existing = transaction.get(reference)
            val currentDelivered = existing.getTimestamp("lastDeliveredAt") ?: Timestamp(0, 0)
            val currentRead = existing.getTimestamp("lastReadAt") ?: Timestamp(0, 0)
            transaction.update(
                reference,
                mapOf(
                    "lastDeliveredAt" to maxTimestamp(maxTimestamp(currentDelivered, currentRead), watermark),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }.await()
        requireLocalAccess(userId, operation.conversationId)
    }

    private fun listenToRecentMessages(
        conversationId: String,
        onCommitted: (List<MessageEntity>) -> Unit,
    ): ListenerRegistration? {
        val firestore = services.firestore ?: return null
        return firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("serverCreatedAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || snapshot.metadata.isFromCache) return@addSnapshotListener
                val committed = snapshot?.documents.orEmpty()
                    .filterNot { it.metadata.hasPendingWrites() }
                    .mapNotNull { it.toMessageEntity(conversationId) }
                if (committed.isNotEmpty()) onCommitted(committed)
            }
    }

    private suspend fun reconcileRecentMessages(
        conversationId: String,
        committed: List<MessageEntity>,
        capturedUserId: String,
    ) {
        val reconciled = database.withTransaction {
            val owner = database.localAccountDao().get()
            if (
                services.auth?.currentUser?.uid != capturedUserId ||
                owner?.ownerUid != capturedUserId ||
                owner.orgId != configuration.organizationId ||
                inboxDao.get(capturedUserId, conversationId) == null
            ) return@withTransaction false
            val mergedMessages = preserveLocalMediaPaths(committed)
            messageDao.upsert(mergedMessages)
            updateMessageSearchIndex(mergedMessages)
            val previousState = conversationStateDao.get(conversationId)
            if (previousState?.oldestLoadedMessageId == null) {
                val oldestCommitted = committed.minWithOrNull(
                    compareBy<MessageEntity> {
                        it.serverCreatedAtMillis ?: it.clientCreatedAtMillis
                    }.thenBy(MessageEntity::messageId),
                )
                conversationStateDao.upsert(
                    ConversationStateEntity(
                        conversationId = conversationId,
                        oldestLoadedMessageId = oldestCommitted?.messageId,
                        hasMoreHistory = committed.size >= PAGE_SIZE,
                        lastSnapshotAtMillis = System.currentTimeMillis(),
                        lastReceiptWriteAtMillis = previousState?.lastReceiptWriteAtMillis,
                    ),
                )
            }
            true
        }
        if (!reconciled) return
        if (services.auth?.currentUser?.uid != capturedUserId) return
        committed.filter { it.senderId != capturedUserId }
            .maxByOrNull { it.serverCreatedAtMillis ?: it.clientCreatedAtMillis }
            ?.let { latest -> markDeliveredLocallyAndRemotely(conversationId, latest, capturedUserId) }
    }

    /** Keep the device-private attachment URI when a Firestore snapshot refreshes its metadata. */
    private suspend fun preserveLocalMediaPaths(remote: List<MessageEntity>): List<MessageEntity> {
        if (remote.isEmpty()) return remote
        val localById = messageDao.get(remote.map(MessageEntity::messageId)).associateBy(MessageEntity::messageId)
        return remote.map { incoming ->
            incoming.copy(localMediaPath = localById[incoming.messageId]?.localMediaPath)
        }
    }

    private suspend fun markDeliveredLocallyAndRemotely(
        conversationId: String,
        message: MessageEntity,
        userId: String,
    ) {
        if (!isCurrentSession(userId)) return
        if (message.senderId == userId) return
        val watermarkMillis = message.serverCreatedAtMillis ?: message.clientCreatedAtMillis
        val now = System.currentTimeMillis()
        database.withTransaction {
            if (!hasLocalAccess(userId, conversationId)) return@withTransaction
            val existing = receiptDao.get(conversationId, userId)
            val delivered = maxOf(existing?.lastDeliveredAtMillis ?: 0L, watermarkMillis)
            receiptDao.upsert(
                listOf(
                    ReceiptEntity(
                        conversationId,
                        userId,
                        delivered,
                        existing?.lastReadAtMillis?.coerceAtMost(delivered),
                        now,
                    ),
                ),
            )
            outboxDao.upsert(
                OutboxEntity(
                    operationId = "delivered:$conversationId",
                    operationType = OutboxOperation.MARK_DELIVERED,
                    conversationId = conversationId,
                    watermarkMessageId = message.messageId,
                    clientCreatedAtMillis = now,
                ),
            )
        }
        if (isCurrentSession(userId)) outboxScheduler.schedule()
    }

    private suspend fun updateMessageSearchIndex(messages: List<MessageEntity>) {
        val searchable = messages.filter { !it.deleted && it.body.isNotBlank() }
        if (searchable.isNotEmpty()) searchDao.index(searchable.map(SearchIndex::message))
        messages.filter { it.deleted || it.body.isBlank() }.forEach { message ->
            searchDao.remove(SearchIndex.MESSAGE, message.messageId)
        }
    }

    private fun sanitizeFileName(raw: String, kind: MessageKind): String {
        val normalized = raw.trim().replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .replace(Regex("\\s+"), "_").take(120)
        return normalized.ifBlank { attachmentDefaultName(kind) }
    }

    private fun attachmentDefaultName(kind: MessageKind): String = when (kind) {
        MessageKind.IMAGE -> "photo.jpg"
        MessageKind.DOCUMENT -> "document.bin"
        MessageKind.AUDIO -> "voice_message.m4a"
        MessageKind.TEXT -> "message.txt"
    }

    private fun attachmentPreview(kind: MessageKind, fileName: String): String = when (kind) {
        MessageKind.IMAGE -> "Photo"
        MessageKind.DOCUMENT -> fileName
        MessageKind.AUDIO -> "Voice message"
        MessageKind.TEXT -> fileName
    }

    /**
     * Copies provider-backed content into private app storage before it enters the durable
     * outbox. This prevents a reboot, process death, or expired picker permission from turning a
     * retryable media message into an unreadable URI.
     */
    private fun persistOutgoingAttachment(command: SendAttachmentCommand, safeName: String): File {
        val source = Uri.parse(command.sourceUri)
        val directory = File(context.filesDir, "outgoing_media/${command.messageId}").apply { mkdirs() }
        val output = File(directory, safeName)
        try {
            val input = when (source.scheme) {
                "file" -> source.path?.let(::File)?.inputStream()
                else -> context.contentResolver.openInputStream(source)
            } ?: error("Selected attachment is unavailable")
            input.use { stream ->
                output.outputStream().buffered().use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        copied += read
                        require(copied <= MAX_ATTACHMENT_BYTES) { "Attachments must be 25 MB or smaller" }
                        target.write(buffer, 0, read)
                    }
                    require(copied > 0L) { "Selected attachment is empty" }
                }
            }
            source.path?.let(::File)?.takeIf { source.scheme == "file" && it.parentFile == context.cacheDir }
                ?.delete()
            return output
        } catch (failure: Exception) {
            output.delete()
            directory.delete()
            throw failure
        }
    }

    private suspend fun isLocalOwner(ownerId: String): Boolean {
        val owner = database.localAccountDao().get()
        return services.auth?.currentUser?.uid == ownerId &&
            owner?.ownerUid == ownerId &&
            owner.orgId == configuration.organizationId
    }

    private suspend fun hasLocalAccess(ownerId: String, conversationId: String): Boolean =
        isLocalOwner(ownerId) &&
            inboxDao.get(ownerId, conversationId)?.orgId == configuration.organizationId

    private suspend fun requireLocalAccess(ownerId: String, conversationId: String) {
        check(hasLocalAccess(ownerId, conversationId)) { "Conversation access changed" }
    }

    private fun requireUserId(): String = services.auth?.currentUser?.uid
        ?: error("An authenticated employee session is required")

    private fun isCurrentSession(ownerId: String): Boolean =
        services.auth?.currentUser?.uid == ownerId

    private fun requireUuidV7(value: String) {
        require(value == value.lowercase(Locale.US)) { "UUIDv7 must be lowercase" }
        val uuid = runCatching { UUID.fromString(value) }.getOrNull() ?: error("Invalid UUID")
        require(uuid.version() == 7) { "Message ID must be UUIDv7" }
        require(UuidV7.timestampMillis(uuid) > 0) { "Invalid UUIDv7 timestamp" }
    }

    private fun maxTimestamp(first: Timestamp, second: Timestamp): Timestamp =
        if (first.compareTo(second) >= 0) first else second

    private companion object {
        const val MAX_ATTACHMENT_BYTES = 25L * 1024L * 1024L
        const val PAGE_SIZE = 50
        const val PAGE_PREFETCH_DISTANCE = 10
        const val MAX_CACHED_PAGES = 5
        const val MAX_TEXT_LENGTH = 4_000
        const val TYPING_TTL_MILLIS = 10_000L
        val SUPPORTED_REACTIONS = setOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🫡", "😊")
    }
}
