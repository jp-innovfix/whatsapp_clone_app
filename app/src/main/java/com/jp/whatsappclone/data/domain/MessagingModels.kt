package com.jp.whatsappclone.data.domain

import com.jp.whatsappclone.data.util.UuidV7

enum class MemberRole { MEMBER, ADMIN }

enum class NotificationPreference { ALL, NONE }

enum class ConversationType { DIRECT, GROUP }

enum class MessageDeliveryStatus { PENDING, SENT, DELIVERED, READ, FAILED }

enum class MessageKind { TEXT, IMAGE, DOCUMENT, AUDIO }

data class Member(
    val id: String,
    val orgId: String,
    val email: String,
    val displayName: String,
    val role: MemberRole,
    val active: Boolean,
    val avatarKey: String? = null,
    val notificationPreference: NotificationPreference = NotificationPreference.ALL,
    val activeUntilMillis: Long? = null,
    val lastSeenAtMillis: Long? = null,
) {
    val notificationsEnabled: Boolean get() = notificationPreference == NotificationPreference.ALL
}

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: Member) : SessionState
    data class Disabled(val reason: String? = null) : SessionState
}

data class Conversation(
    val id: String,
    val orgId: String,
    val type: ConversationType,
    val memberIds: List<String>,
    val title: String? = null,
    val createdBy: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class InboxItem(
    val conversationId: String,
    val ownerId: String,
    val type: ConversationType,
    val title: String,
    val lastMessageId: String? = null,
    val lastMessageSenderId: String? = null,
    val preview: String = "",
    val lastActivityAtMillis: Long = 0,
    val unreadCount: Int = 0,
    val muted: Boolean = false,
    val pinned: Boolean = false,
    val archived: Boolean = false,
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val clientCreatedAtMillis: Long,
    val serverCreatedAtMillis: Long? = null,
    val replyToMessageId: String? = null,
    /**
     * Transient Room projection of the referenced message. These fields are never persisted on
     * the reply document; they are populated by a self-join so a tombstone on the original is
     * reflected when a paged reply is rebound.
     */
    val replySenderId: String? = null,
    val replyBody: String? = null,
    val replyDeleted: Boolean = false,
    val deleted: Boolean = false,
    val schemaVersion: Int = 1,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.PENDING,
    val failureReason: String? = null,
    val kind: MessageKind = MessageKind.TEXT,
    val storagePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val durationMillis: Long? = null,
    val localMediaPath: String? = null,
)

data class Reaction(
    val conversationId: String,
    val messageId: String,
    val userId: String,
    val emoji: String,
    val updatedAtMillis: Long,
)

data class Receipt(
    val conversationId: String,
    val memberId: String,
    val lastDeliveredAtMillis: Long? = null,
    val lastReadAtMillis: Long? = null,
)

data class SendTextCommand(
    val conversationId: String,
    val body: String,
    val replyToMessageId: String? = null,
    val messageId: String = UuidV7.newString(),
    val clientCreatedAtMillis: Long = System.currentTimeMillis(),
)

data class SendAttachmentCommand(
    val conversationId: String,
    val sourceUri: String,
    val kind: MessageKind,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val durationMillis: Long? = null,
    val caption: String? = null,
    val replyToMessageId: String? = null,
    val messageId: String = UuidV7.newString(),
    val clientCreatedAtMillis: Long = System.currentTimeMillis(),
)

data class MessagePageRequest(
    val conversationId: String,
    val beforeMessageId: String? = null,
    val limit: Int = 50,
)

data class MessagePageResult(
    val loadedCount: Int,
    val hasMore: Boolean,
)

enum class LocalSearchEntityType { MEMBER, INBOX, MESSAGE }

data class LocalSearchMatch(
    val entityType: LocalSearchEntityType,
    val entityId: String,
    val conversationId: String? = null,
)
