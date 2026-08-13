package com.jp.whatsappclone.data.remote

import com.google.firebase.Timestamp

/** Firestore wire models. Keep property names aligned with Firestore Rules and Functions. */
data class MemberDocumentDto(
    val email: String = "",
    val displayName: String = "",
    val role: String = "MEMBER",
    val active: Boolean = false,
    val avatarKey: String? = null,
    val notificationPreference: String = "ALL",
    val lastSeenAt: Timestamp? = null,
    val activeUntil: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

data class DeviceDocumentDto(
    val installationId: String = "",
    val token: String = "",
    val platform: String = "ANDROID",
    val enabled: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

data class InboxDocumentDto(
    val conversationId: String = "",
    val orgId: String = "",
    val conversationType: String = "DIRECT",
    val title: String = "",
    val lastMessageId: String? = null,
    val lastMessagePreview: String = "",
    val lastMessageSenderId: String? = null,
    val lastMessageAt: Timestamp? = null,
    val unreadCount: Long = 0,
    val muted: Boolean = false,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val updatedAt: Timestamp? = null,
)

data class ConversationDocumentDto(
    val orgId: String = "",
    val type: String = "DIRECT",
    val memberIds: List<String> = emptyList(),
    val title: String? = null,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val schemaVersion: Long = 1,
)

data class ConversationMemberDocumentDto(
    val userId: String = "",
    val role: String = "MEMBER",
    val joinedAt: Timestamp? = null,
    val lastDeliveredAt: Timestamp? = null,
    val lastReadAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

data class MessageDocumentDto(
    val senderId: String = "",
    val text: String = "",
    val clientCreatedAt: Timestamp? = null,
    val serverCreatedAt: Timestamp? = null,
    val replyToMessageId: String? = null,
    val deletedAt: Timestamp? = null,
    val schemaVersion: Long = 1,
    val kind: String = "TEXT",
    val storagePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val durationMillis: Long? = null,
)

data class ReactionDocumentDto(
    val messageId: String = "",
    val userId: String = "",
    val emoji: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

data class TypingDocumentDto(
    val userId: String = "",
    val isTyping: Boolean = false,
    val expiresAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

data class DirectConversationResponseDto(
    val conversationId: String,
    val created: Boolean,
)

data class GroupConversationResponseDto(
    val conversationId: String = "",
)
