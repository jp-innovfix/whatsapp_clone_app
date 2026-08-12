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
enum class DeliveryStatus { None, Sent, Delivered, Read }

data class MessageUi(
    val id: String,
    val direction: MessageDirection,
    val body: String = "",
    val time: String,
    val status: DeliveryStatus = DeliveryStatus.None,
    val reaction: String? = null,
    val deleted: Boolean = false,
    val voiceSeconds: Int? = null,
    val audioPath: String? = null,
    val replyToSender: String? = null,
    val replyToBody: String? = null,
    val senderName: String? = null,
)

data class ChatThreadUi(
    val id: String,
    val contact: ContactUi,
    val preview: String,
    val time: String,
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val draft: Boolean = false,
    val mediaLabel: String? = null,
    val messages: List<MessageUi> = emptyList(),
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
)

private val mediaFilters = setOf("Photos", "Videos", "Links", "GIFs", "Audio", "Documents", "Stickers", "Polls")

fun AppState.filteredSearchChats(): List<ChatThreadUi> {
    val query = searchQuery.trim()
    val selectedMedia = searchFilters intersect mediaFilters
    return chats.filter { thread ->
        val searchableText = buildString {
            append(thread.contact.name)
            append(' ')
            append(thread.preview)
            append(' ')
            append(thread.mediaLabel.orEmpty())
            append(' ')
            thread.messages.forEach { append(it.body).append(' ') }
        }
        val textMatches = query.isBlank() || searchableText.contains(query, ignoreCase = true)
        val unreadMatches = "Unread" !in searchFilters || thread.unreadCount > 0
        val contactMatches = "Contacts" !in searchFilters || (thread.contact.isSaved && !thread.contact.isGroup)
        val nonContactMatches = "Non-contacts" !in searchFilters || !thread.contact.isSaved
        val mediaMatches = selectedMedia.isEmpty() || selectedMedia.any { filter ->
            when (filter) {
                "Photos" -> searchableText.contains("photo", ignoreCase = true)
                "Videos" -> searchableText.contains("video", ignoreCase = true)
                "Links" -> searchableText.contains("http", ignoreCase = true)
                "GIFs" -> searchableText.contains("gif", ignoreCase = true)
                "Audio" -> thread.messages.any { it.voiceSeconds != null } || searchableText.contains("voice", ignoreCase = true)
                "Documents" -> searchableText.contains("document", ignoreCase = true) || searchableText.contains(".pdf", ignoreCase = true)
                "Stickers" -> searchableText.contains("sticker", ignoreCase = true)
                "Polls" -> searchableText.contains("poll", ignoreCase = true)
                else -> true
            }
        }
        textMatches && unreadMatches && contactMatches && nonContactMatches && mediaMatches
    }
}

fun AppState.filteredHomeChats(): List<ChatThreadUi> = chats.filter { thread ->
    val unreadMatches = "Unread" !in searchFilters || thread.unreadCount > 0
    val favouriteMatches = "Favourites" !in searchFilters || thread.pinned
    val groupMatches = "Groups" !in searchFilters || thread.contact.isGroup
    val customListMatches = "Bang meet" !in searchFilters ||
        thread.contact.name.contains("Bengaluru", ignoreCase = true) ||
        thread.contact.name.contains("INNOVFIX", ignoreCase = true)
    unreadMatches && favouriteMatches && groupMatches && customListMatches
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
    data class SetMessageReaction(val threadId: String, val messageId: String, val reaction: String?) : AppAction
    data class DeleteMessage(val threadId: String, val messageId: String) : AppAction
    data object DismissDraftAd : AppAction
    data object ToggleMessageMute : AppAction
    data object ToggleStatusMute : AppAction
    data class StartCall(val contactId: String, val video: Boolean = false) : AppAction
}

@Serializable
sealed interface AppDestination : NavKey {
    @Serializable data object Login : AppDestination
    @Serializable data object Main : AppDestination
    @Serializable data class Chat(val threadId: String) : AppDestination
    @Serializable data class GroupInfo(val threadId: String) : AppDestination
    @Serializable data object Search : AppDestination
    @Serializable data object ContactPicker : AppDestination
    @Serializable data object Notifications : AppDestination
}
