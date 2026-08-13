package com.jp.whatsappclone.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import com.jp.whatsappclone.BuildConfig
import com.jp.whatsappclone.data.AppAction
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.data.ChatThreadUi
import com.jp.whatsappclone.data.ContactUi
import com.jp.whatsappclone.data.DeliveryStatus
import com.jp.whatsappclone.data.GroupMemberUi
import com.jp.whatsappclone.data.MessageDirection
import com.jp.whatsappclone.data.MessageUi
import com.jp.whatsappclone.data.ReactionSummaryUi
import com.jp.whatsappclone.data.domain.ConversationType
import com.jp.whatsappclone.data.domain.Conversation
import com.jp.whatsappclone.data.domain.InboxItem
import com.jp.whatsappclone.data.domain.LocalSearchEntityType
import com.jp.whatsappclone.data.domain.Member
import com.jp.whatsappclone.data.domain.Message
import com.jp.whatsappclone.data.domain.MessageDeliveryStatus
import com.jp.whatsappclone.data.domain.Reaction
import com.jp.whatsappclone.data.domain.Receipt
import com.jp.whatsappclone.data.domain.SendTextCommand
import com.jp.whatsappclone.data.domain.SendAttachmentCommand
import com.jp.whatsappclone.data.domain.SessionState
import com.jp.whatsappclone.data.repository.AuthRepository
import com.jp.whatsappclone.data.repository.ConversationRepository
import com.jp.whatsappclone.data.repository.DirectoryRepository
import com.jp.whatsappclone.data.repository.MessagingRepository
import com.jp.whatsappclone.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val session: SessionState = SessionState.Loading,
    val submitting: Boolean = false,
    val errorMessage: String? = null,
    val firebaseConfigured: Boolean = BuildConfig.FIREBASE_CONFIGURED,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {
    private val submitting = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AuthUiState> = combine(
        repository.observeSession(),
        submitting,
        error,
    ) { session, isSubmitting, errorMessage ->
        AuthUiState(session, isSubmitting, errorMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthUiState())

    init {
        ensureAnonymousSession()
    }

    fun ensureAnonymousSession() {
        if (submitting.value || !BuildConfig.FIREBASE_CONFIGURED) return
        viewModelScope.launch {
            submitting.value = true
            error.value = null
            repository.ensureAnonymousSession()
                .onFailure { error.value = it.safeMessage("Unable to start INNOVFIX") }
            submitting.value = false
        }
    }

    fun signIn(email: String, password: String) {
        if (submitting.value || !BuildConfig.FIREBASE_CONFIGURED) return
        viewModelScope.launch {
            submitting.value = true
            error.value = null
            repository.signIn(email, password)
                .onFailure { error.value = it.safeMessage("Unable to sign in") }
            submitting.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            error.value = null
            repository.signOut()
        }
    }
}

data class HomeUiState(
    val appState: AppState = AppState(),
    val currentUserId: String? = null,
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    directoryRepository: DirectoryRepository,
    private val conversationRepository: ConversationRepository,
    private val messagingRepository: MessagingRepository,
    private val searchRepository: SearchRepository,
) : ViewModel() {
    private val localUi = MutableStateFlow(AppState())
    private val error = MutableStateFlow<String?>(null)

    private val inboxState = conversationRepository.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val conversationsById = inboxState
        .map { inbox -> inbox.map(InboxItem::conversationId).distinct() }
        .distinctUntilChanged()
        .flatMapLatest { conversationIds ->
            if (conversationIds.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(conversationIds.map(conversationRepository::observeConversation)) { conversations ->
                    conversations.filterNotNull().associateBy(Conversation::id)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private data class HomeConversationState(
        val inbox: List<InboxItem>,
        val conversationsById: Map<String, Conversation>,
    )

    private val homeConversations = combine(inboxState, conversationsById, ::HomeConversationState)

    private data class DownloadedSearchState(
        val query: String = "",
        val conversationIds: Set<String> = emptySet(),
        val memberIds: Set<String> = emptySet(),
    )

    private val downloadedSearch = localUi
        .map { it.searchQuery.trim() }
        .distinctUntilChanged()
        .mapLatest { query ->
            if (query.isBlank()) return@mapLatest DownloadedSearchState()
            delay(150)
            val matches = runCatching { searchRepository.searchDownloaded(query) }.getOrElse { emptyList() }
            DownloadedSearchState(
                query = query,
                conversationIds = matches.mapNotNullTo(mutableSetOf()) { match ->
                    when (match.entityType) {
                        LocalSearchEntityType.INBOX -> match.entityId
                        LocalSearchEntityType.MESSAGE -> match.conversationId
                        LocalSearchEntityType.MEMBER -> null
                    }
                },
                memberIds = matches.filter { it.entityType == LocalSearchEntityType.MEMBER }
                    .mapTo(mutableSetOf()) { it.entityId },
            )
        }
        .onStart { emit(DownloadedSearchState()) }

    private val localUiWithSearch = combine(localUi, downloadedSearch) { state, search ->
        state.copy(
            downloadedSearchQuery = search.query,
            downloadedSearchConversationIds = search.conversationIds,
            downloadedSearchMemberIds = search.memberIds,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.observeSession(),
        directoryRepository.observeActiveMembers(),
        homeConversations,
        localUiWithSearch,
        error,
    ) { session, members, conversationState, legacy, errorMessage ->
        val signedIn = session as? SessionState.SignedIn
        if (signedIn == null) {
            HomeUiState(
                appState = legacy,
                loading = session == SessionState.Loading,
                errorMessage = errorMessage,
            )
        } else {
            val contactsById = members.associate { it.id to it.toContactUi() }
            val groupMembers = conversationState.conversationsById.values
                .asSequence()
                .filter { it.type == ConversationType.GROUP }
                .flatMap { conversation ->
                    conversation.memberIds.asSequence().mapNotNull { memberId ->
                        contactsById[memberId]?.let { contact ->
                            GroupMemberUi(
                                groupId = conversation.id,
                                contact = contact,
                                isAdmin = memberId == conversation.createdBy,
                            )
                        }
                    }
                }
                .toList()
            HomeUiState(
                appState = legacy.copy(
                    contacts = members.filter { it.id != signedIn.user.id }.map(Member::toContactUi),
                    chats = conversationState.inbox.map { item ->
                        item.toChatThreadUi(
                            contacts = contactsById,
                            conversation = conversationState.conversationsById[item.conversationId],
                            currentUserId = signedIn.user.id,
                        )
                    },
                    groupMembers = groupMembers,
                    messageNotificationsMuted = !signedIn.user.notificationsEnabled,
                ),
                currentUserId = signedIn.user.id,
                loading = false,
                errorMessage = errorMessage,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun dispatch(action: AppAction) {
        when (action) {
            is AppAction.SendText,
            is AppAction.SendAttachment,
            is AppAction.RetryMessage,
            is AppAction.SetMessageReaction,
            is AppAction.DeleteMessage
            -> Unit // Destination-scoped ChatViewModel owns message mutations.
            AppAction.ToggleMessageMute -> viewModelScope.launch {
                authRepository.setNotificationsEnabled(uiState.value.appState.messageNotificationsMuted)
                    .onFailure { error.value = it.safeMessage("Unable to update notification preference") }
            }
            is AppAction.SetConversationMuted -> viewModelScope.launch {
                conversationRepository.setMuted(action.conversationId, action.muted)
                    .onFailure { error.value = it.safeMessage("Unable to update conversation notifications") }
            }
            is AppAction.SetConversationPinned -> viewModelScope.launch {
                conversationRepository.setPinned(action.conversationId, action.pinned)
                    .onFailure { error.value = it.safeMessage("Unable to update pinned conversation") }
            }
            is AppAction.SetConversationArchived -> viewModelScope.launch {
                conversationRepository.setArchived(action.conversationId, action.archived)
                    .onFailure { error.value = it.safeMessage("Unable to update archived conversation") }
            }
            else -> localUi.value = reduceLocalShell(localUi.value, action)
        }
    }

    suspend fun openDirectConversation(targetUserId: String): Result<String> =
        conversationRepository.getOrCreateDirectConversation(targetUserId)
            .onFailure { error.value = it.safeMessage("Unable to open conversation") }

    suspend fun createGroup(title: String, memberIds: List<String>): Result<String> =
        conversationRepository.createGroup(title, memberIds)
            .onFailure { error.value = it.safeMessage("Unable to create group") }

    suspend fun forwardMessage(
        sourceConversationId: String,
        messageId: String,
        targetConversationId: String,
    ): Result<String> = messagingRepository.forwardMessage(sourceConversationId, messageId, targetConversationId)
        .onFailure { error.value = it.safeMessage("Unable to forward message") }

    fun clearError() {
        error.value = null
    }
}

private fun reduceLocalShell(state: AppState, action: AppAction): AppState = when (action) {
    is AppAction.SelectTab -> state.copy(selectedTab = action.tab)
    is AppAction.SetSearchQuery -> state.copy(searchQuery = action.query)
    is AppAction.ToggleSearchFilter -> {
        val filters = state.searchFilters.toMutableSet().apply {
            if (action.filter == "All") clear() else if (!add(action.filter)) remove(action.filter)
        }
        state.copy(searchFilters = filters)
    }
    AppAction.DismissDraftAd -> state.copy(draftAdVisible = false)
    AppAction.ToggleMessageMute -> state
    AppAction.ToggleStatusMute -> state.copy(statusNotificationsMuted = !state.statusNotificationsMuted)
    is AppAction.SetConversationMuted,
    is AppAction.SetConversationPinned,
    is AppAction.SetConversationArchived,
    -> state
    AppAction.ToggleArchivedView -> state.copy(showArchived = !state.showArchived)
    else -> state
}

data class ChatUiState(
    val conversationId: String? = null,
    val draft: String = "",
    val typingMemberNames: Set<String> = emptySet(),
    val loading: Boolean = true,
    val loadingOlder: Boolean = false,
    val hasMoreHistory: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val directoryRepository: DirectoryRepository,
    private val conversationRepository: ConversationRepository,
    private val messagingRepository: MessagingRepository,
) : ViewModel() {
    private val conversationId = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private var draftSaveJob: Job? = null
    private var readReceiptJob: Job? = null
    private var typingJob: Job? = null
    private var typingRefreshJob: Job? = null
    private var lastTypingWriteAt = 0L
    private var typingPublished = false
    private var currentComposerText = ""
    private val historyState = MutableStateFlow(HistoryLoadState())

    private data class HistoryLoadState(
        val loading: Boolean = false,
        val hasMore: Boolean = true,
    )

    private data class RemoteChatState(
        val conversationId: String?,
        val messages: List<Message>,
        val reactions: List<Reaction>,
        val receipts: List<Receipt>,
        val typingIds: Set<String>,
        val draft: String,
        val conversation: Conversation? = null,
    )

    private val emptyRemoteState = RemoteChatState(null, emptyList(), emptyList(), emptyList(), emptySet(), "")

    private val remoteState: StateFlow<RemoteChatState> = conversationId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyRemoteState)
        } else {
            combine(
                messagingRepository.observeCachedMessages(id),
                messagingRepository.observeReactions(id),
                messagingRepository.observeReceipts(id),
                messagingRepository.observeTyping(id),
                messagingRepository.observeDraft(id),
            ) { messages, reactions, receipts, typingIds, draft ->
                RemoteChatState(id, messages, reactions, receipts, typingIds, draft)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyRemoteState)

    private val remoteStateWithConversation: Flow<RemoteChatState> = combine(
        remoteState,
        conversationId.flatMapLatest { id ->
            if (id == null) flowOf(null) else conversationRepository.observeConversation(id)
        },
    ) { remote, conversation ->
        remote.copy(conversation = conversation?.takeIf { it.id == remote.conversationId })
    }

    private data class MessageUiContext(
        val conversationId: String?,
        val currentUserId: String?,
        val members: Map<String, Member>,
        val reactionsByMessage: Map<String, List<Reaction>>,
        val expectedRecipientIds: Set<String>,
        val receiptsByMember: Map<String, Receipt>,
    )

    private val messageUiContext: Flow<MessageUiContext> = combine(
        authRepository.observeSession(),
        directoryRepository.observeActiveMembers(),
        remoteStateWithConversation,
    ) { session, members, remote ->
        val currentUserId = (session as? SessionState.SignedIn)?.user?.id
        val membersById = members.associateBy(Member::id)
        MessageUiContext(
            conversationId = remote.conversationId,
            currentUserId = currentUserId,
            members = membersById,
            reactionsByMessage = remote.reactions.groupBy(Reaction::messageId),
            expectedRecipientIds = remote.conversation?.memberIds.orEmpty()
                .filterTo(linkedSetOf()) { it != currentUserId && it in membersById },
            receiptsByMember = remote.receipts.associateBy(Receipt::memberId),
        )
    }

    /**
     * The timeline is a bounded PagingData stream backed only by Room. Firestore continues to
     * reconcile the newest 50 records through [remoteState], while scroll-boundary loads insert
     * older pages into Room and invalidate this source.
     */
    val messages: Flow<PagingData<MessageUi>> = conversationId.flatMapLatest { id ->
        if (id == null) {
            flowOf(PagingData.empty())
        } else {
            // Cache the Room generation before combining it with frequently changing receipt and
            // reaction context. PagingData is single-consumer until cached, and that context can
            // legitimately remap the same generation many times.
            val cachedRoomMessages = messagingRepository.observeMessages(id).cachedIn(viewModelScope)
            combine(
                cachedRoomMessages,
                messageUiContext.filter { it.conversationId == id },
            ) { pagingData, context ->
                pagingData.map { message ->
                    message.toMessageUi(
                        currentUserId = context.currentUserId,
                        members = context.members,
                        messagesById = emptyMap(),
                        reactions = context.reactionsByMessage[message.id].orEmpty(),
                        expectedRecipientIds = context.expectedRecipientIds,
                        receiptsByMember = context.receiptsByMember,
                    )
                }
            }
        }
    }.cachedIn(viewModelScope)

    private val coreUiState: Flow<ChatUiState> = combine(
        conversationId,
        authRepository.observeSession(),
        directoryRepository.observeActiveMembers(),
        remoteStateWithConversation,
        error,
    ) { id, session, members, remote, errorMessage ->
        val currentUserId = (session as? SessionState.SignedIn)?.user?.id
        val membersById = members.associateBy(Member::id)
        val activeRemote = remote.takeIf { it.conversationId == id } ?: emptyRemoteState
        ChatUiState(
            conversationId = id,
            draft = activeRemote.draft,
            typingMemberNames = activeRemote.typingIds
                .filter { it != currentUserId }
                .mapNotNull { membersById[it]?.displayName }
                .toSet(),
            loading = id == null,
            errorMessage = errorMessage,
        )
    }

    val uiState: StateFlow<ChatUiState> = combine(coreUiState, historyState) { state, history ->
        state.copy(loadingOlder = history.loading, hasMoreHistory = history.hasMore)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    fun bind(conversationId: String) {
        if (this.conversationId.value == conversationId) return
        val previous = this.conversationId.value
        if (previous != null && typingPublished) {
            viewModelScope.launch { messagingRepository.setTyping(previous, false) }
        }
        this.conversationId.value = conversationId
        currentComposerText = ""
        typingPublished = false
        lastTypingWriteAt = 0
        historyState.value = HistoryLoadState()
        error.value = null
    }

    fun loadOlder() {
        val boundId = conversationId.value ?: return
        val history = historyState.value
        if (history.loading || !history.hasMore) return
        historyState.value = history.copy(loading = true)
        viewModelScope.launch {
            messagingRepository.pageMessages(
                com.jp.whatsappclone.data.domain.MessagePageRequest(
                    conversationId = boundId,
                ),
            ).onSuccess { page ->
                if (conversationId.value == boundId) {
                    historyState.value = HistoryLoadState(loading = false, hasMore = page.hasMore)
                }
            }.onFailure { failure ->
                if (conversationId.value == boundId) {
                    // Stop the scroll boundary from immediately retrying forever while offline.
                    // Reopening the destination resets history paging and provides a safe retry.
                    historyState.value = HistoryLoadState(loading = false, hasMore = false)
                    error.value = failure.safeMessage("Unable to load older messages")
                }
            }
        }
    }

    fun dispatch(action: AppAction) {
        val boundId = conversationId.value ?: return
        viewModelScope.launch {
            val result = when (action) {
                is AppAction.SendText -> messagingRepository.sendText(
                    SendTextCommand(
                        conversationId = boundId,
                        body = action.text,
                        replyToMessageId = action.replyToMessageId,
                    ),
                ).map { Unit }
                is AppAction.SendAttachment -> messagingRepository.sendAttachment(
                    SendAttachmentCommand(
                        conversationId = boundId,
                        sourceUri = action.sourceUri,
                        kind = action.kind,
                        fileName = action.fileName,
                        mimeType = action.mimeType,
                        sizeBytes = action.sizeBytes,
                        durationMillis = action.durationMillis,
                        caption = action.caption,
                        replyToMessageId = action.replyToMessageId,
                    ),
                ).map { Unit }
                is AppAction.RetryMessage -> messagingRepository.retry(boundId, action.messageId)
                is AppAction.SetMessageReaction -> messagingRepository.setReaction(
                    conversationId = boundId,
                    messageId = action.messageId,
                    reaction = action.reaction,
                )
                is AppAction.DeleteMessage -> messagingRepository.deleteForEveryone(boundId, action.messageId)
                else -> return@launch
            }
            result.onFailure { error.value = it.safeMessage("Messaging action failed") }
            if (result.isSuccess && (action is AppAction.SendText || action is AppAction.SendAttachment)) {
                currentComposerText = ""
                messagingRepository.saveDraft(boundId, "")
                publishTyping(boundId, false)
            }
        }
    }

    suspend fun prepareAttachment(messageId: String): Result<String> =
        messagingRepository.prepareAttachment(messageId)

    fun onDraftChanged(body: String) {
        val boundId = conversationId.value ?: return
        currentComposerText = body.take(4_000)
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(200)
            messagingRepository.saveDraft(boundId, currentComposerText)
        }

        val shouldType = currentComposerText.isNotBlank()
        val now = System.currentTimeMillis()
        if (!shouldType) {
            if (typingPublished || typingJob?.isActive == true) publishTyping(boundId, false)
            typingRefreshJob?.cancel()
            typingRefreshJob = null
        } else if (shouldType && (!typingPublished || now - lastTypingWriteAt >= 3_000)) {
            publishTyping(boundId, true)
        }
        if (shouldType && typingRefreshJob?.isActive != true) {
            typingRefreshJob = viewModelScope.launch {
                while (conversationId.value == boundId && currentComposerText.isNotBlank()) {
                    delay(3_000)
                    if (conversationId.value == boundId && currentComposerText.isNotBlank()) {
                        publishTyping(boundId, true)
                    }
                }
            }
        }
    }

    fun markVisible(throughMessageId: String) {
        val boundId = conversationId.value ?: return
        readReceiptJob?.cancel()
        readReceiptJob = viewModelScope.launch {
            delay(250)
            if (conversationId.value != boundId) return@launch
            messagingRepository.markRead(boundId, throughMessageId)
                .onFailure { error.value = it.safeMessage("Unable to update read receipt") }
        }
    }

    fun unbind() {
        val boundId = conversationId.value ?: return
        currentComposerText = ""
        if (typingPublished) publishTyping(boundId, false)
        typingRefreshJob?.cancel()
        typingRefreshJob = null
    }

    fun clearError() {
        error.value = null
    }

    private fun publishTyping(conversationId: String, isTyping: Boolean) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            messagingRepository.setTyping(conversationId, isTyping)
                .onSuccess {
                    typingPublished = isTyping
                    lastTypingWriteAt = System.currentTimeMillis()
                }
        }
    }
}

private fun Member.toContactUi(): ContactUi {
    val now = System.currentTimeMillis()
    val online = activeUntilMillis?.let { it > now } == true
    return ContactUi(
        id = id,
        name = displayName,
        initials = displayName.initials(),
        color = stableAvatarColor(id),
        subtitle = when {
            online -> "online"
            lastSeenAtMillis != null -> "last seen ${lastSeenAtMillis.formatDayAndTime()}"
            else -> "Available"
        },
    )
}

private fun InboxItem.toChatThreadUi(
    contacts: Map<String, ContactUi>,
    conversation: Conversation?,
    currentUserId: String,
): ChatThreadUi {
    val directContactId = conversation?.takeIf { it.type == ConversationType.DIRECT }
        ?.memberIds
        ?.firstOrNull { it != currentUserId }
    val directContact = directContactId?.let(contacts::get)
        ?: contacts.values.firstOrNull { it.name == title }
    val contact = directContact ?: ContactUi(
        id = conversationId,
        name = title.ifBlank { if (type == ConversationType.GROUP) "Group" else "Conversation" },
        initials = title.initials(),
        color = stableAvatarColor(conversationId),
        isGroup = type == ConversationType.GROUP,
    )
    return ChatThreadUi(
        id = conversationId,
        contact = contact.copy(isGroup = type == ConversationType.GROUP),
        preview = preview,
        lastMessageOutgoing = lastMessageSenderId == currentUserId,
        time = lastActivityAtMillis.takeIf { it > 0 }?.formatInboxTime().orEmpty(),
        unreadCount = unreadCount,
        pinned = pinned,
        muted = muted,
        archived = archived,
    )
}

private fun Message.toMessageUi(
    currentUserId: String?,
    members: Map<String, Member>,
    messagesById: Map<String, Message>,
    reactions: List<Reaction>,
    expectedRecipientIds: Set<String>,
    receiptsByMember: Map<String, Receipt>,
): MessageUi {
    val timestamp = serverCreatedAtMillis ?: clientCreatedAtMillis
    val original = replyToMessageId?.let(messagesById::get)
    val projectedReplySenderId = original?.senderId ?: replySenderId
    val projectedReplyBody = when {
        original?.deleted == true || (original == null && replyDeleted) -> "This message was deleted"
        original != null -> original.body
        else -> replyBody
    }
    val derivedStatus = statusFromReceipts(this, currentUserId, expectedRecipientIds, receiptsByMember)
    val uniqueReactions = reactions.distinctBy(Reaction::userId)
    val ownReaction = uniqueReactions.firstOrNull { it.userId == currentUserId }?.emoji
    val reactionSummaries = uniqueReactions
        .groupingBy(Reaction::emoji)
        .eachCount()
        .map { (emoji, count) -> ReactionSummaryUi(emoji, count) }
        .sortedWith(
            compareByDescending<ReactionSummaryUi>(ReactionSummaryUi::count)
                .thenBy { summary ->
                    SUPPORTED_REACTION_ORDER.indexOf(summary.emoji).let { if (it < 0) Int.MAX_VALUE else it }
                }
                .thenBy(ReactionSummaryUi::emoji),
        )
    return MessageUi(
        id = id,
        direction = if (senderId == currentUserId) MessageDirection.Outgoing else MessageDirection.Incoming,
        body = body,
        time = timestamp.formatClockTime(),
        status = derivedStatus.toUiStatus(),
        ownReaction = ownReaction.takeUnless { deleted },
        reactionSummaries = reactionSummaries.takeUnless { deleted }.orEmpty(),
        deleted = deleted,
        replyToMessageId = replyToMessageId,
        replyToSender = projectedReplySenderId?.let { if (it == currentUserId) "You" else members[it]?.displayName },
        replyToBody = projectedReplyBody,
        senderName = members[senderId]?.displayName,
        clientCreatedAtMillis = timestamp,
        failureReason = failureReason,
        kind = kind,
        storagePath = storagePath,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        durationMillis = durationMillis,
        voiceSeconds = if (kind == com.jp.whatsappclone.data.domain.MessageKind.AUDIO) {
            ((durationMillis ?: 0L) / 1_000L).coerceAtLeast(1L).toInt()
        } else null,
        audioPath = localMediaPath,
        mediaPath = localMediaPath,
    )
}

private val SUPPORTED_REACTION_ORDER = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🫡", "😊")

private fun statusFromReceipts(
    message: Message,
    currentUserId: String?,
    expectedRecipientIds: Set<String>,
    receiptsByMember: Map<String, Receipt>,
): MessageDeliveryStatus {
    if (message.senderId != currentUserId || message.status in setOf(MessageDeliveryStatus.PENDING, MessageDeliveryStatus.FAILED)) {
        return message.status
    }
    if (expectedRecipientIds.isEmpty()) return message.status
    val messageTime = message.serverCreatedAtMillis ?: message.clientCreatedAtMillis
    return when {
        expectedRecipientIds.all { memberId ->
            (receiptsByMember[memberId]?.lastReadAtMillis ?: Long.MIN_VALUE) >= messageTime
        } -> MessageDeliveryStatus.READ
        expectedRecipientIds.all { memberId ->
            (receiptsByMember[memberId]?.lastDeliveredAtMillis ?: Long.MIN_VALUE) >= messageTime
        } -> MessageDeliveryStatus.DELIVERED
        else -> MessageDeliveryStatus.SENT
    }
}

private fun MessageDeliveryStatus.toUiStatus(): DeliveryStatus = when (this) {
    MessageDeliveryStatus.PENDING -> DeliveryStatus.Pending
    MessageDeliveryStatus.SENT -> DeliveryStatus.Sent
    MessageDeliveryStatus.DELIVERED -> DeliveryStatus.Delivered
    MessageDeliveryStatus.READ -> DeliveryStatus.Read
    MessageDeliveryStatus.FAILED -> DeliveryStatus.Failed
}

private fun String.initials(): String = trim().split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .joinToString("")
    .ifBlank { "?" }

private fun stableAvatarColor(seed: String): Long {
    val colors = longArrayOf(0xFF31576E, 0xFF6E443B, 0xFF395E48, 0xFF68324C, 0xFF455A64, 0xFF704456)
    return colors[(seed.hashCode().toLong() and Long.MAX_VALUE).rem(colors.size).toInt()]
}

private fun Long.formatClockTime(): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this)).lowercase(Locale.getDefault())

private fun Long.formatDayAndTime(): String =
    SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(this)).lowercase(Locale.getDefault())

private fun Long.formatInboxTime(): String {
    val day = SimpleDateFormat("yyyyMMdd", Locale.US)
    return if (day.format(Date(this)) == day.format(Date())) formatClockTime()
    else SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(this))
}

private fun Throwable.safeMessage(fallback: String): String = when {
    this is FirebaseFirestoreException &&
        code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "This Firebase account is not an active INNOVFIX employee. Contact an administrator."

    this is StorageException && errorCode == StorageException.ERROR_NOT_AUTHORIZED ->
        "Firebase Storage denied this media upload. Storage authorization must be enabled."

    this is StorageException && errorCode == StorageException.ERROR_QUOTA_EXCEEDED ->
        "Firebase Storage quota is unavailable. Try again later."

    else -> message?.takeIf { it.isNotBlank() }?.take(180) ?: fallback
}
