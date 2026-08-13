package com.jp.whatsappclone.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.Fts4
import androidx.room.Index

@Entity(tableName = "local_account")
data class LocalAccountEntity(
    @androidx.room.PrimaryKey val slot: Int = SINGLETON_SLOT,
    @ColumnInfo(name = "owner_uid") val ownerUid: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "self_profile_validated_at_millis") val selfProfileValidatedAtMillis: Long? = null,
) {
    companion object {
        const val SINGLETON_SLOT = 0
    }
}

@Entity(
    tableName = "members",
    indices = [Index("org_id"), Index("active"), Index("display_name")],
)
data class MemberEntity(
    @androidx.room.PrimaryKey @ColumnInfo(name = "member_id") val memberId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val email: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val role: String,
    val active: Boolean,
    @ColumnInfo(name = "avatar_key") val avatarKey: String? = null,
    @ColumnInfo(name = "notification_preference") val notificationPreference: String = "ALL",
    @ColumnInfo(name = "active_until_millis") val activeUntilMillis: Long? = null,
    @ColumnInfo(name = "last_seen_at_millis") val lastSeenAtMillis: Long? = null,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long,
)

@Entity(
    tableName = "conversations",
    indices = [Index("org_id"), Index("updated_at_millis")],
)
data class ConversationEntity(
    @androidx.room.PrimaryKey @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val type: String,
    @ColumnInfo(name = "member_ids") val memberIds: List<String>,
    val title: String? = null,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int = 1,
)

@Entity(
    tableName = "inbox",
    primaryKeys = ["owner_id", "conversation_id"],
    indices = [Index("last_activity_at_millis"), Index("unread_count")],
)
data class InboxEntity(
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val type: String,
    val title: String,
    @ColumnInfo(name = "last_message_id") val lastMessageId: String? = null,
    @ColumnInfo(name = "last_message_preview") val lastMessagePreview: String = "",
    @ColumnInfo(name = "last_message_sender_id") val lastMessageSenderId: String? = null,
    @ColumnInfo(name = "last_activity_at_millis") val lastActivityAtMillis: Long = 0,
    @ColumnInfo(name = "unread_count") val unreadCount: Int = 0,
    val muted: Boolean = false,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long,
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversation_id", "client_created_at_millis"]),
        Index(value = ["conversation_id", "server_created_at_millis"]),
        Index("sender_id"),
        Index("reply_to_message_id"),
    ],
)
data class MessageEntity(
    @androidx.room.PrimaryKey @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "sender_id") val senderId: String,
    val body: String,
    @ColumnInfo(name = "client_created_at_millis") val clientCreatedAtMillis: Long,
    @ColumnInfo(name = "server_created_at_millis") val serverCreatedAtMillis: Long? = null,
    @ColumnInfo(name = "reply_to_message_id") val replyToMessageId: String? = null,
    val deleted: Boolean = false,
    @ColumnInfo(name = "deleted_at_millis") val deletedAtMillis: Long? = null,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int = 1,
    @ColumnInfo(name = "local_status") val localStatus: String,
    @ColumnInfo(name = "failure_reason") val failureReason: String? = null,
    val kind: String = "TEXT",
    @ColumnInfo(name = "storage_path") val storagePath: String? = null,
    @ColumnInfo(name = "file_name") val fileName: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long? = null,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long? = null,
    @ColumnInfo(name = "local_media_path") val localMediaPath: String? = null,
)

/** A read-only projection used by Paging; it does not change the database schema. */
data class MessagePageRow(
    @Embedded val message: MessageEntity,
    @ColumnInfo(name = "reply_message_id") val replyMessageId: String? = null,
    @ColumnInfo(name = "reply_sender_id") val replySenderId: String? = null,
    @ColumnInfo(name = "reply_body") val replyBody: String? = null,
    @ColumnInfo(name = "reply_deleted") val replyDeleted: Boolean? = null,
)

@Entity(
    tableName = "reactions",
    primaryKeys = ["message_id", "user_id"],
    indices = [Index("conversation_id")],
)
data class ReactionEntity(
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val emoji: String,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long,
)

@Entity(
    tableName = "receipts",
    primaryKeys = ["conversation_id", "member_id"],
    indices = [Index("member_id")],
)
data class ReceiptEntity(
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "member_id") val memberId: String,
    @ColumnInfo(name = "last_delivered_at_millis") val lastDeliveredAtMillis: Long? = null,
    @ColumnInfo(name = "last_read_at_millis") val lastReadAtMillis: Long? = null,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long,
)

enum class OutboxOperation { SEND_MESSAGE, SET_REACTION, DELETE_MESSAGE, MARK_DELIVERED, MARK_READ, SET_TYPING }
enum class OutboxState { PENDING, PROCESSING, FAILED }

@Entity(
    tableName = "outbox",
    indices = [Index(value = ["state", "next_attempt_at_millis"]), Index("conversation_id")],
)
data class OutboxEntity(
    @androidx.room.PrimaryKey @ColumnInfo(name = "operation_id") val operationId: String,
    @ColumnInfo(name = "operation_type") val operationType: OutboxOperation,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "message_id") val messageId: String? = null,
    val body: String? = null,
    val kind: String = "TEXT",
    @ColumnInfo(name = "storage_path") val storagePath: String? = null,
    @ColumnInfo(name = "file_name") val fileName: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long? = null,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long? = null,
    @ColumnInfo(name = "local_media_path") val localMediaPath: String? = null,
    @ColumnInfo(name = "reply_to_message_id") val replyToMessageId: String? = null,
    val reaction: String? = null,
    @ColumnInfo(name = "watermark_message_id") val watermarkMessageId: String? = null,
    @ColumnInfo(name = "typing_value") val typingValue: Boolean? = null,
    @ColumnInfo(name = "client_created_at_millis") val clientCreatedAtMillis: Long,
    val state: OutboxState = OutboxState.PENDING,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int = 0,
    @ColumnInfo(name = "next_attempt_at_millis") val nextAttemptAtMillis: Long = 0,
    @ColumnInfo(name = "last_error_code") val lastErrorCode: String? = null,
)

@Entity(tableName = "drafts", indices = [Index("owner_id")])
data class DraftEntity(
    @androidx.room.PrimaryKey @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    val body: String,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long,
)

@Entity(tableName = "conversation_state")
data class ConversationStateEntity(
    @androidx.room.PrimaryKey @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "oldest_loaded_message_id") val oldestLoadedMessageId: String? = null,
    @ColumnInfo(name = "has_more_history") val hasMoreHistory: Boolean = true,
    @ColumnInfo(name = "last_snapshot_at_millis") val lastSnapshotAtMillis: Long? = null,
    @ColumnInfo(name = "last_receipt_write_at_millis") val lastReceiptWriteAtMillis: Long? = null,
)

@Fts4
@Entity(tableName = "search_fts")
data class SearchFtsEntity(
    @androidx.room.PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String?,
    @ColumnInfo(name = "searchable_text") val searchableText: String,
)
