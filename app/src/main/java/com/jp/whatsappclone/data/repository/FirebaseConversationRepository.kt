package com.jp.whatsappclone.data.repository

import androidx.room.withTransaction
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.jp.whatsappclone.data.domain.Conversation
import com.jp.whatsappclone.data.domain.InboxItem
import com.jp.whatsappclone.data.domain.SessionState
import com.jp.whatsappclone.data.local.ConversationDao
import com.jp.whatsappclone.data.local.InboxDao
import com.jp.whatsappclone.data.local.InnovfixDatabase
import com.jp.whatsappclone.data.local.SearchDao
import com.jp.whatsappclone.data.local.SearchIndex
import com.jp.whatsappclone.data.mapper.toConversationEntity
import com.jp.whatsappclone.data.mapper.toDomain
import com.jp.whatsappclone.data.mapper.toInboxEntity
import com.jp.whatsappclone.data.remote.FirebaseServices
import com.jp.whatsappclone.data.remote.BackendConfiguration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseConversationRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val services: FirebaseServices,
    private val configuration: BackendConfiguration,
    private val inboxDao: InboxDao,
    private val conversationDao: ConversationDao,
    private val database: InnovfixDatabase,
    private val searchDao: SearchDao,
) : ConversationRepository {
    override fun observeInbox(): Flow<List<InboxItem>> = authRepository.observeSession().flatMapLatest { session ->
        val userId = (session as? SessionState.SignedIn)?.user?.id
        if (userId == null) flowOf(emptyList()) else observeInboxForUser(userId)
    }

    private fun observeInboxForUser(userId: String): Flow<List<InboxItem>> = channelFlow {
        launch { inboxDao.observe(userId).collect { send(it.map { entity -> entity.toDomain() }) } }
        val listener: ListenerRegistration? = services.firestore
            ?.collection("members")
            ?.document(userId)
            ?.collection("inbox")
            ?.orderBy("pinned", Query.Direction.DESCENDING)
            ?.orderBy("lastMessageAt", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, _ ->
                val currentSnapshot = snapshot
                    ?.takeUnless { it.metadata.isFromCache }
                    ?: return@addSnapshotListener
                launch {
                    database.withTransaction {
                        val owner = database.localAccountDao().get()
                        if (
                            services.auth?.currentUser?.uid != userId ||
                            owner?.ownerUid != userId ||
                            owner.orgId != configuration.organizationId
                        ) return@withTransaction
                        currentSnapshot.documentChanges.forEach { change ->
                            val entity = change.document.toInboxEntity(userId)
                            if (change.type == DocumentChange.Type.REMOVED || entity == null) {
                                database.purgeConversation(userId, change.document.id)
                            } else {
                                inboxDao.upsert(entity)
                                searchDao.index(SearchIndex.inbox(entity))
                            }
                        }
                        val remoteIds = currentSnapshot.documents.mapTo(mutableSetOf()) { it.id }
                        inboxDao.ids(userId)
                            .filterNot(remoteIds::contains)
                            .forEach { database.purgeConversation(userId, it) }
                    }
                }
            }
        awaitClose { listener?.remove() }
    }

    override fun observeConversation(conversationId: String): Flow<Conversation?> = channelFlow {
        val ownerId = services.auth?.currentUser?.uid
        launch { conversationDao.observe(conversationId).map { it?.toDomain() }.collect(::send) }
        val listener = services.firestore
            ?.collection("conversations")
            ?.document(conversationId)
            ?.addSnapshotListener { snapshot, _ ->
                snapshot?.takeUnless { it.metadata.isFromCache }?.toConversationEntity()?.let { entity ->
                    if (ownerId != null) launch {
                        database.withTransaction {
                            val owner = database.localAccountDao().get()
                            if (
                                services.auth?.currentUser?.uid == ownerId &&
                                owner?.ownerUid == ownerId &&
                                owner.orgId == configuration.organizationId &&
                                inboxDao.get(ownerId, conversationId) != null
                            ) conversationDao.upsert(entity)
                        }
                    }
                }
            }
        awaitClose { listener?.remove() }
    }

    override suspend fun getOrCreateDirectConversation(targetUserId: String): Result<String> = runCatching {
        require(targetUserId.isNotBlank()) { "Target employee is required" }
        val ownerId = services.auth?.currentUser?.uid ?: error("Sign in is required")
        val functions = services.functions ?: error("Firebase is not configured for this build")
        val firestore = services.firestore ?: error("Firebase is not configured for this build")
        val data = functions.getHttpsCallable("getOrCreateDirectConversation")
            .call(mapOf("targetUserId" to targetUserId))
            .await()
            .data as? Map<*, *> ?: error("Invalid server response")
        val conversationId = data["conversationId"] as? String ?: error("Missing conversation ID")
        check(services.auth?.currentUser?.uid == ownerId) { "Employee session changed" }
        val conversation = firestore.collection("conversations")
            .document(conversationId)
            .get()
            .await()
            .toConversationEntity()
            ?: error("Conversation is unavailable")
        val inbox = firestore.collection("members")
            .document(ownerId)
            .collection("inbox")
            .document(conversationId)
            .get()
            .await()
            .toInboxEntity(ownerId)
            ?: error("Conversation inbox is unavailable")
        database.withTransaction {
            val owner = database.localAccountDao().get()
            check(
                services.auth?.currentUser?.uid == ownerId &&
                    owner?.ownerUid == ownerId &&
                    owner.orgId == configuration.organizationId,
            ) { "Employee session changed" }
            conversationDao.upsert(conversation)
            inboxDao.upsert(inbox)
            searchDao.index(SearchIndex.inbox(inbox))
        }
        conversationId
    }

    override suspend fun createGroup(title: String, memberIds: List<String>): Result<String> = runCatching {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Group name is required" }
        require(normalizedTitle.length <= 120) { "Group name cannot exceed 120 characters" }
        require(memberIds.distinct().isNotEmpty()) { "Select at least one employee" }
        val ownerId = services.auth?.currentUser?.uid ?: error("Sign in is required")
        val functions = services.functions ?: error("Firebase is not configured for this build")
        val firestore = services.firestore ?: error("Firebase is not configured for this build")
        val data = functions.getHttpsCallable("createGroupConversation")
            .call(mapOf("title" to normalizedTitle, "memberIds" to memberIds.distinct()))
            .await()
            .data as? Map<*, *> ?: error("Invalid server response")
        val conversationId = data["conversationId"] as? String ?: error("Missing conversation ID")
        check(services.auth?.currentUser?.uid == ownerId) { "Employee session changed" }
        val conversation = firestore.collection("conversations").document(conversationId)
            .get().await().toConversationEntity() ?: error("Group is unavailable")
        val inbox = firestore.collection("members").document(ownerId).collection("inbox")
            .document(conversationId).get().await().toInboxEntity(ownerId)
            ?: error("Group inbox is unavailable")
        database.withTransaction {
            check(hasCurrentOwner(ownerId)) { "Employee session changed" }
            conversationDao.upsert(conversation)
            inboxDao.upsert(inbox)
            searchDao.index(SearchIndex.inbox(inbox))
        }
        conversationId
    }

    override suspend fun setMuted(conversationId: String, muted: Boolean): Result<Unit> =
        updateInboxPreference(conversationId, "muted", muted)

    override suspend fun setPinned(conversationId: String, pinned: Boolean): Result<Unit> =
        updateInboxPreference(conversationId, "pinned", pinned)

    override suspend fun setArchived(conversationId: String, archived: Boolean): Result<Unit> =
        updateInboxPreference(conversationId, "archived", archived)

    private suspend fun updateInboxPreference(
        conversationId: String,
        field: String,
        value: Boolean,
    ): Result<Unit> = runCatching {
        require(conversationId.isNotBlank()) { "Conversation is required" }
        val userId = services.auth?.currentUser?.uid ?: error("Sign in is required")
        val firestore = services.firestore ?: error("Firebase is not configured for this build")
        val previous = inboxDao.get(userId, conversationId)
            ?: error("Conversation is not available for this account")
        val now = System.currentTimeMillis()
        when (field) {
            "muted" -> inboxDao.setMuted(userId, conversationId, value, now)
            "pinned" -> inboxDao.setPinned(userId, conversationId, value, now)
            "archived" -> inboxDao.setArchived(userId, conversationId, value, now)
            else -> error("Unsupported inbox preference")
        }
        runCatching {
            firestore.collection("members")
                .document(userId)
                .collection("inbox")
                .document(conversationId)
                .update(
                    mapOf(
                        field to value,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                .await()
        }.onFailure {
            when (field) {
                "muted" -> inboxDao.setMuted(userId, conversationId, previous.muted, previous.updatedAtMillis)
                "pinned" -> inboxDao.setPinned(userId, conversationId, previous.pinned, previous.updatedAtMillis)
                "archived" -> inboxDao.setArchived(userId, conversationId, previous.archived, previous.updatedAtMillis)
            }
        }.getOrThrow()
    }

    private suspend fun hasCurrentOwner(ownerId: String): Boolean {
        val owner = database.localAccountDao().get()
        return services.auth?.currentUser?.uid == ownerId &&
            owner?.ownerUid == ownerId && owner.orgId == configuration.organizationId
    }

}
