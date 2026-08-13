package com.jp.whatsappclone.data.mapper

import com.google.firebase.firestore.DocumentSnapshot
import com.jp.whatsappclone.data.domain.Conversation
import com.jp.whatsappclone.data.domain.ConversationType
import com.jp.whatsappclone.data.domain.InboxItem
import com.jp.whatsappclone.data.domain.Member
import com.jp.whatsappclone.data.domain.MemberRole
import com.jp.whatsappclone.data.domain.Message
import com.jp.whatsappclone.data.domain.MessageDeliveryStatus
import com.jp.whatsappclone.data.domain.MessageKind
import com.jp.whatsappclone.data.domain.NotificationPreference
import com.jp.whatsappclone.data.domain.Reaction
import com.jp.whatsappclone.data.domain.Receipt
import com.jp.whatsappclone.data.local.ConversationEntity
import com.jp.whatsappclone.data.local.InboxEntity
import com.jp.whatsappclone.data.local.MemberEntity
import com.jp.whatsappclone.data.local.MessageEntity
import com.jp.whatsappclone.data.local.MessagePageRow
import com.jp.whatsappclone.data.local.ReactionEntity
import com.jp.whatsappclone.data.local.ReceiptEntity
import com.jp.whatsappclone.data.remote.ConversationDocumentDto
import com.jp.whatsappclone.data.remote.ConversationMemberDocumentDto
import com.jp.whatsappclone.data.remote.InboxDocumentDto
import com.jp.whatsappclone.data.remote.MemberDocumentDto
import com.jp.whatsappclone.data.remote.MessageDocumentDto
import com.jp.whatsappclone.data.remote.ReactionDocumentDto

fun MemberEntity.toDomain() = Member(
    id = memberId,
    orgId = orgId,
    email = email,
    displayName = displayName,
    role = enumValueOrDefault(role, MemberRole.MEMBER),
    active = active,
    avatarKey = avatarKey,
    notificationPreference = enumValueOrDefault(notificationPreference, NotificationPreference.ALL),
    activeUntilMillis = activeUntilMillis,
    lastSeenAtMillis = lastSeenAtMillis,
)

fun DocumentSnapshot.toMemberEntity(orgId: String): MemberEntity? {
    val dto = toObject(MemberDocumentDto::class.java) ?: return null
    if (dto.email.isBlank() || dto.displayName.isBlank()) return null
    return MemberEntity(
        memberId = id,
        orgId = orgId,
        email = dto.email,
        displayName = dto.displayName,
        role = dto.role,
        active = dto.active,
        avatarKey = dto.avatarKey,
        notificationPreference = dto.notificationPreference,
        activeUntilMillis = dto.activeUntil?.toDate()?.time,
        lastSeenAtMillis = dto.lastSeenAt?.toDate()?.time,
        updatedAtMillis = dto.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
    )
}

fun ConversationEntity.toDomain() = Conversation(
    id = conversationId,
    orgId = orgId,
    type = enumValueOrDefault(type, ConversationType.DIRECT),
    memberIds = memberIds,
    title = title,
    createdBy = createdBy,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun DocumentSnapshot.toConversationEntity(): ConversationEntity? {
    val dto = toObject(ConversationDocumentDto::class.java) ?: return null
    if (dto.orgId.isBlank() || dto.memberIds.isEmpty()) return null
    return ConversationEntity(
        conversationId = id,
        orgId = dto.orgId,
        type = dto.type,
        memberIds = dto.memberIds,
        title = dto.title,
        createdBy = dto.createdBy,
        createdAtMillis = dto.createdAt?.toDate()?.time ?: 0,
        updatedAtMillis = dto.updatedAt?.toDate()?.time ?: 0,
        schemaVersion = dto.schemaVersion.toInt(),
    )
}

fun InboxEntity.toDomain() = InboxItem(
    conversationId = conversationId,
    ownerId = ownerId,
    type = enumValueOrDefault(type, ConversationType.DIRECT),
    title = title,
    lastMessageId = lastMessageId,
    lastMessageSenderId = lastMessageSenderId,
    preview = lastMessagePreview,
    lastActivityAtMillis = lastActivityAtMillis,
    unreadCount = unreadCount,
    muted = muted,
    pinned = pinned,
    archived = archived,
)

fun DocumentSnapshot.toInboxEntity(ownerId: String): InboxEntity? {
    val dto = toObject(InboxDocumentDto::class.java) ?: return null
    if (dto.conversationId.isBlank() || dto.orgId.isBlank()) return null
    return InboxEntity(
        ownerId = ownerId,
        conversationId = dto.conversationId,
        orgId = dto.orgId,
        type = dto.conversationType,
        title = dto.title,
        lastMessageId = dto.lastMessageId,
        lastMessagePreview = dto.lastMessagePreview,
        lastMessageSenderId = dto.lastMessageSenderId,
        lastActivityAtMillis = dto.lastMessageAt?.toDate()?.time ?: 0,
        unreadCount = dto.unreadCount.coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        muted = dto.muted,
        pinned = dto.pinned,
        archived = dto.archived,
        updatedAtMillis = dto.updatedAt?.toDate()?.time ?: 0,
    )
}

fun MessageEntity.toDomain() = Message(
    id = messageId,
    conversationId = conversationId,
    senderId = senderId,
    body = body,
    clientCreatedAtMillis = clientCreatedAtMillis,
    serverCreatedAtMillis = serverCreatedAtMillis,
    replyToMessageId = replyToMessageId,
    deleted = deleted,
    schemaVersion = schemaVersion,
    status = enumValueOrDefault(localStatus, MessageDeliveryStatus.SENT),
    failureReason = failureReason,
    kind = enumValueOrDefault(kind, MessageKind.TEXT),
    storagePath = storagePath,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    durationMillis = durationMillis,
    localMediaPath = localMediaPath,
)

fun MessagePageRow.toDomain() = message.toDomain().copy(
    replySenderId = replySenderId.takeIf { replyMessageId != null },
    replyBody = replyBody.takeIf { replyMessageId != null },
    replyDeleted = replyDeleted == true,
)

fun DocumentSnapshot.toMessageEntity(conversationId: String): MessageEntity? {
    val dto = toObject(MessageDocumentDto::class.java) ?: return null
    if (dto.senderId.isBlank() || dto.clientCreatedAt == null) return null
    return MessageEntity(
        messageId = id,
        conversationId = conversationId,
        senderId = dto.senderId,
        body = if (dto.deletedAt == null) dto.text else "",
        clientCreatedAtMillis = dto.clientCreatedAt.toDate().time,
        serverCreatedAtMillis = dto.serverCreatedAt?.toDate()?.time,
        replyToMessageId = dto.replyToMessageId,
        deleted = dto.deletedAt != null,
        deletedAtMillis = dto.deletedAt?.toDate()?.time,
        schemaVersion = dto.schemaVersion.toInt(),
        localStatus = MessageDeliveryStatus.SENT.name,
        kind = dto.kind,
        storagePath = dto.storagePath,
        fileName = dto.fileName,
        mimeType = dto.mimeType,
        sizeBytes = dto.sizeBytes,
        durationMillis = dto.durationMillis,
    )
}

fun ReactionEntity.toDomain() = Reaction(
    conversationId = conversationId,
    messageId = messageId,
    userId = userId,
    emoji = emoji,
    updatedAtMillis = updatedAtMillis,
)

fun DocumentSnapshot.toReactionEntity(conversationId: String): ReactionEntity? {
    val dto = toObject(ReactionDocumentDto::class.java) ?: return null
    if (dto.messageId.isBlank() || dto.userId.isBlank() || dto.emoji.isBlank()) return null
    return ReactionEntity(
        conversationId = conversationId,
        messageId = dto.messageId,
        userId = dto.userId,
        emoji = dto.emoji,
        createdAtMillis = dto.createdAt?.toDate()?.time ?: 0,
        updatedAtMillis = dto.updatedAt?.toDate()?.time ?: 0,
    )
}

fun ReceiptEntity.toDomain() = Receipt(
    conversationId = conversationId,
    memberId = memberId,
    lastDeliveredAtMillis = lastDeliveredAtMillis,
    lastReadAtMillis = lastReadAtMillis,
)

fun DocumentSnapshot.toReceiptEntity(conversationId: String): ReceiptEntity? {
    val dto = toObject(ConversationMemberDocumentDto::class.java) ?: return null
    if (dto.userId.isBlank()) return null
    return ReceiptEntity(
        conversationId = conversationId,
        memberId = dto.userId,
        lastDeliveredAtMillis = dto.lastDeliveredAt?.toDate()?.time,
        lastReadAtMillis = dto.lastReadAt?.toDate()?.time,
        updatedAtMillis = dto.updatedAt?.toDate()?.time ?: 0,
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
