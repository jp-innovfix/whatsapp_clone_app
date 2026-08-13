package com.jp.whatsappclone.data

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

enum class MainTab { Chats, Calls, Updates, Tools }

data class ContactUi(
    val id: String,
    val name: String,
    val initials: String,
    val color: Long,
    val subtitle: String = "Available",
    val isGroup: Boolean = false,
    val isSaved: Boolean = true,
)

enum class MessageDirection { Incoming, Outgoing }
enum class DeliveryStatus { None, Pending, Sent, Delivered, Read, Failed }

data class ReactionSummaryUi(
    val emoji: String,
    val count: Int,
)

data class MessageUi(
    val id: String,
    val direction: MessageDirection,
    val body: String = "",
    val time: String,
    val status: DeliveryStatus = DeliveryStatus.None,
    val ownReaction: String? = null,
    val reactionSummaries: List<ReactionSummaryUi> = emptyList(),
    val deleted: Boolean = false,
    val voiceSeconds: Int? = null,
    val audioPath: String? = null,
    val replyToSender: String? = null,
    val replyToBody: String? = null,
    val replyToMessageId: String? = null,
    val senderName: String? = null,
    val clientCreatedAtMillis: Long = 0,
    val failureReason: String? = null,
    val kind: com.jp.whatsappclone.data.domain.MessageKind = com.jp.whatsappclone.data.domain.MessageKind.TEXT,
    val storagePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val durationMillis: Long? = null,
    val mediaPath: String? = null,
)

data class ChatThreadUi(
    val id: String,
    val contact: ContactUi,
    val preview: String,
    val time: String,
    val lastMessageOutgoing: Boolean = false,
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val draft: Boolean = false,
    val mediaLabel: String? = null,
    val archived: Boolean = false,
)

data class GroupMemberUi(
    val groupId: String,
    val contact: ContactUi,
    val isAdmin: Boolean = false,
    val about: String = "",
)

enum class CallDirection { Incoming, Outgoing, Missed }

data class CallLogUi(
    val id: String,
    val contact: ContactUi,
    val direction: CallDirection,
    val whenLabel: String,
    val video: Boolean = false,
)

data class StatusUi(
    val id: String,
    val contact: ContactUi,
    val caption: String,
    val gradientStart: Long,
    val gradientEnd: Long,
    val mine: Boolean = false,
)

data class ChannelUi(
    val id: String,
    val name: String,
    val preview: String,
    val time: String,
    val unreadCount: Int,
    val color: Long,
)

data class BusinessMetricUi(
    val label: String,
    val value: String,
    val icon: String,
    val positive: Boolean = false,
)

data class ToolItemUi(
    val title: String,
    val subtitle: String,
    val icon: String,
    val accent: Boolean = false,
)

data class RecordingUi(
    val active: Boolean = false,
    val paused: Boolean = false,
    val seconds: Int = 0,
)

data class AppState(
    val selectedTab: MainTab = MainTab.Chats,
    val contacts: List<ContactUi> = emptyList(),
    val chats: List<ChatThreadUi> = emptyList(),
    val groupMembers: List<GroupMemberUi> = emptyList(),
    val calls: List<CallLogUi> = emptyList(),
    val statuses: List<StatusUi> = emptyList(),
    val channels: List<ChannelUi> = emptyList(),
    val metrics: List<BusinessMetricUi> = emptyList(),
    val toolItems: List<ToolItemUi> = emptyList(),
    val searchQuery: String = "",
    val searchFilters: Set<String> = emptySet(),
    val recording: RecordingUi = RecordingUi(),
    val draftAdVisible: Boolean = true,
    val messageNotificationsMuted: Boolean = false,
    val statusNotificationsMuted: Boolean = true,
    val downloadedSearchQuery: String = "",
    val downloadedSearchConversationIds: Set<String> = emptySet(),
    val downloadedSearchMemberIds: Set<String> = emptySet(),
    val showArchived: Boolean = false,
)

fun AppState.filteredSearchChats(): List<ChatThreadUi> {
    val query = searchQuery.trim()
    val downloadedResultsReady = downloadedSearchQuery.equals(query, ignoreCase = true)
    return chats.filter { thread ->
        val searchableText = buildString {
            append(thread.contact.name)
            append(' ')
            append(thread.preview)
            append(' ')
            append(thread.mediaLabel.orEmpty())
        }
        val textMatches = query.isBlank() || if (downloadedResultsReady) {
            thread.id in downloadedSearchConversationIds || thread.contact.id in downloadedSearchMemberIds
        } else {
            searchableText.contains(query, ignoreCase = true)
        }
        val unreadMatches = "Unread" !in searchFilters || thread.unreadCount > 0
        val contactMatches = "Contacts" !in searchFilters || (thread.contact.isSaved && !thread.contact.isGroup)
        val groupMatches = "Groups" !in searchFilters || thread.contact.isGroup
        textMatches && unreadMatches && contactMatches && groupMatches
    }
}

fun AppState.filteredSearchContacts(): List<ContactUi> {
    val query = searchQuery.trim()
    if (query.isBlank() || "Groups" in searchFilters) return emptyList()
    val downloadedResultsReady = downloadedSearchQuery.equals(query, ignoreCase = true)
    return contacts.filter { contact ->
        if (downloadedResultsReady) contact.id in downloadedSearchMemberIds
        else contact.name.contains(query, ignoreCase = true) || contact.subtitle.contains(query, ignoreCase = true)
    }
}

fun AppState.filteredHomeChats(): List<ChatThreadUi> = chats.filter { thread ->
    val unreadMatches = "Unread" !in searchFilters || thread.unreadCount > 0
    val favouriteMatches = "Favourites" !in searchFilters || thread.pinned
    val groupMatches = "Groups" !in searchFilters || thread.contact.isGroup
    val archiveMatches = thread.archived == showArchived
    unreadMatches && favouriteMatches && groupMatches && archiveMatches
}

sealed interface AppAction {
    data class SelectTab(val tab: MainTab) : AppAction
    data class SetSearchQuery(val query: String) : AppAction
    data class ToggleSearchFilter(val filter: String) : AppAction
    data class SendText(
        val threadId: String,
        val text: String,
        val replyToSender: String? = null,
        val replyToBody: String? = null,
        val replyToMessageId: String? = null,
    ) : AppAction
    data object StartRecording : AppAction
    data object TickRecording : AppAction
    data object ToggleRecordingPause : AppAction
    data object CancelRecording : AppAction
    data class SendRecording(
        val threadId: String,
        val audioPath: String? = null,
        val durationSeconds: Int? = null,
    ) : AppAction
    data class SendAttachment(
        val threadId: String,
        val sourceUri: String,
        val kind: com.jp.whatsappclone.data.domain.MessageKind,
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long? = null,
        val durationMillis: Long? = null,
        val caption: String? = null,
        val replyToMessageId: String? = null,
    ) : AppAction
    data class SetMessageReaction(val threadId: String, val messageId: String, val reaction: String?) : AppAction
    data class DeleteMessage(val threadId: String, val messageId: String) : AppAction
    data class RetryMessage(val threadId: String, val messageId: String) : AppAction
    data object DismissDraftAd : AppAction
    data object ToggleMessageMute : AppAction
    data object ToggleStatusMute : AppAction
    data class SetConversationMuted(val conversationId: String, val muted: Boolean) : AppAction
    data class SetConversationPinned(val conversationId: String, val pinned: Boolean) : AppAction
    data class SetConversationArchived(val conversationId: String, val archived: Boolean) : AppAction
    data object ToggleArchivedView : AppAction
    data class StartCall(val contactId: String, val video: Boolean = false) : AppAction
}

@Serializable
sealed interface AppDestination : NavKey {
    @Serializable data object Login : AppDestination
    @Serializable data object Main : AppDestination
    @Serializable data class Chat(
        val threadId: String,
        val title: String? = null,
        val contactId: String? = null,
    ) : AppDestination
    @Serializable data class ActiveCall(val contactId: String, val video: Boolean = false) : AppDestination
    @Serializable data class CallResult(val contactId: String) : AppDestination
    @Serializable data class GroupInfo(val threadId: String) : AppDestination
    @Serializable data object Search : AppDestination
    @Serializable data object ContactPicker : AppDestination
    @Serializable data object NewGroup : AppDestination
    @Serializable data class ForwardMessage(val conversationId: String, val messageId: String) : AppDestination
    @Serializable data object Notifications : AppDestination
}
