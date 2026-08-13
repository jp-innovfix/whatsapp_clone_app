package com.jp.whatsappclone.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAccountDao {
    @Query("SELECT * FROM local_account WHERE slot = 0 LIMIT 1")
    suspend fun get(): LocalAccountEntity?

    @Upsert
    suspend fun upsert(item: LocalAccountEntity)

    @Query("DELETE FROM local_account")
    suspend fun clear()
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE active = 1 ORDER BY display_name COLLATE NOCASE")
    fun observeActive(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE member_id = :memberId LIMIT 1")
    suspend fun get(memberId: String): MemberEntity?

    @Query("SELECT member_id FROM members")
    suspend fun ids(): List<String>

    @Query("SELECT * FROM members WHERE active = 1 ORDER BY display_name COLLATE NOCASE")
    suspend fun active(): List<MemberEntity>

    @Upsert suspend fun upsert(items: List<MemberEntity>)
    @Upsert suspend fun upsert(item: MemberEntity)
    @Query("DELETE FROM members WHERE member_id = :memberId") suspend fun delete(memberId: String)
    @Query("DELETE FROM members") suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<MemberEntity>) {
        clear()
        if (items.isNotEmpty()) upsert(items)
    }
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE conversation_id = :conversationId LIMIT 1")
    fun observe(conversationId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE conversation_id = :conversationId LIMIT 1")
    suspend fun get(conversationId: String): ConversationEntity?

    @Upsert suspend fun upsert(item: ConversationEntity)
    @Upsert suspend fun upsert(items: List<ConversationEntity>)
    @Query("DELETE FROM conversations WHERE conversation_id = :conversationId")
    suspend fun delete(conversationId: String)
    @Query("DELETE FROM conversations") suspend fun clear()
}

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox WHERE owner_id = :ownerId ORDER BY pinned DESC, last_activity_at_millis DESC")
    fun observe(ownerId: String): Flow<List<InboxEntity>>

    @Upsert suspend fun upsert(items: List<InboxEntity>)
    @Upsert suspend fun upsert(item: InboxEntity)
    @Query("DELETE FROM inbox WHERE owner_id = :ownerId AND conversation_id = :conversationId")
    suspend fun delete(ownerId: String, conversationId: String)
    @Query("SELECT * FROM inbox WHERE owner_id = :ownerId AND conversation_id = :conversationId LIMIT 1")
    suspend fun get(ownerId: String, conversationId: String): InboxEntity?
    @Query("SELECT conversation_id FROM inbox WHERE owner_id = :ownerId")
    suspend fun ids(ownerId: String): List<String>
    @Query("SELECT * FROM inbox WHERE owner_id = :ownerId")
    suspend fun all(ownerId: String): List<InboxEntity>
    @Query("SELECT * FROM inbox")
    suspend fun all(): List<InboxEntity>
    @Query(
        "UPDATE inbox SET unread_count = 0, updated_at_millis = :updatedAtMillis " +
            "WHERE owner_id = :ownerId AND conversation_id = :conversationId",
    )
    suspend fun markRead(ownerId: String, conversationId: String, updatedAtMillis: Long)
    @Query(
        "UPDATE inbox SET muted = :muted, updated_at_millis = :updatedAtMillis " +
            "WHERE owner_id = :ownerId AND conversation_id = :conversationId",
    )
    suspend fun setMuted(ownerId: String, conversationId: String, muted: Boolean, updatedAtMillis: Long)
    @Query(
        "UPDATE inbox SET pinned = :pinned, updated_at_millis = :updatedAtMillis " +
            "WHERE owner_id = :ownerId AND conversation_id = :conversationId",
    )
    suspend fun setPinned(ownerId: String, conversationId: String, pinned: Boolean, updatedAtMillis: Long)
    @Query(
        "UPDATE inbox SET archived = :archived, updated_at_millis = :updatedAtMillis " +
            "WHERE owner_id = :ownerId AND conversation_id = :conversationId",
    )
    suspend fun setArchived(ownerId: String, conversationId: String, archived: Boolean, updatedAtMillis: Long)
    @Query("DELETE FROM inbox WHERE owner_id = :ownerId") suspend fun clear(ownerId: String)
    @Query("DELETE FROM inbox") suspend fun clear()

    @Transaction
    suspend fun replaceForOwner(ownerId: String, items: List<InboxEntity>) {
        clear(ownerId)
        if (items.isNotEmpty()) upsert(items)
    }
}

@Dao
interface MessageDao {
    @Query(
        "SELECT message.*, original.message_id AS reply_message_id, " +
            "original.sender_id AS reply_sender_id, original.body AS reply_body, " +
            "original.deleted AS reply_deleted FROM messages AS message " +
            "LEFT JOIN messages AS original ON original.message_id = message.reply_to_message_id " +
            "WHERE message.conversation_id = :conversationId " +
            "ORDER BY COALESCE(message.server_created_at_millis, message.client_created_at_millis) DESC, " +
            "message.message_id DESC",
    )
    fun pagingSource(conversationId: String): PagingSource<Int, MessagePageRow>

    @Query(
        "SELECT * FROM messages WHERE conversation_id = :conversationId " +
            "ORDER BY COALESCE(server_created_at_millis, client_created_at_millis) DESC, message_id DESC LIMIT :limit",
    )
    fun observeRecent(conversationId: String, limit: Int = 50): Flow<List<MessageEntity>>

    @Query(
        "SELECT * FROM messages WHERE conversation_id = :conversationId " +
            "ORDER BY COALESCE(server_created_at_millis, client_created_at_millis) ASC, message_id ASC",
    )
    fun observeAllAscending(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE message_id = :messageId LIMIT 1")
    suspend fun get(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE message_id IN (:messageIds)")
    suspend fun get(messageIds: List<String>): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE deleted = 0")
    suspend fun searchable(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(item: MessageEntity): Long

    @Upsert suspend fun upsert(items: List<MessageEntity>)

    @Query(
        "UPDATE messages SET local_status = :status, failure_reason = :failureReason, " +
            "server_created_at_millis = COALESCE(:serverCreatedAtMillis, server_created_at_millis) " +
            "WHERE message_id = :messageId",
    )
    suspend fun updateStatus(
        messageId: String,
        status: String,
        failureReason: String? = null,
        serverCreatedAtMillis: Long? = null,
    )

    @Query("UPDATE messages SET local_media_path = :localMediaPath WHERE message_id = :messageId")
    suspend fun setLocalMediaPath(messageId: String, localMediaPath: String)

    @Query(
        "UPDATE messages SET body = '', deleted = 1, deleted_at_millis = :deletedAtMillis " +
            "WHERE message_id = :messageId",
    )
    suspend fun markDeleted(messageId: String, deletedAtMillis: Long)

    @Query("DELETE FROM messages WHERE conversation_id = :conversationId")
    suspend fun clear(conversationId: String)
    @Query("DELETE FROM messages") suspend fun clear()
}

@Dao
interface ReactionDao {
    @Query("SELECT * FROM reactions WHERE conversation_id = :conversationId")
    fun observe(conversationId: String): Flow<List<ReactionEntity>>

    @Upsert suspend fun upsert(item: ReactionEntity)
    @Upsert suspend fun upsert(items: List<ReactionEntity>)
    @Query("DELETE FROM reactions WHERE message_id = :messageId AND user_id = :userId")
    suspend fun delete(messageId: String, userId: String)
    @Query("DELETE FROM reactions") suspend fun clear()
    @Query("DELETE FROM reactions WHERE conversation_id = :conversationId")
    suspend fun clear(conversationId: String)

    @Transaction
    suspend fun replaceForConversation(conversationId: String, items: List<ReactionEntity>) {
        clear(conversationId)
        if (items.isNotEmpty()) upsert(items)
    }
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts WHERE conversation_id = :conversationId")
    fun observe(conversationId: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE conversation_id = :conversationId AND member_id = :memberId LIMIT 1")
    suspend fun get(conversationId: String, memberId: String): ReceiptEntity?

    @Upsert suspend fun upsert(items: List<ReceiptEntity>)
    @Query("DELETE FROM receipts") suspend fun clear()
    @Query("DELETE FROM receipts WHERE conversation_id = :conversationId")
    suspend fun clear(conversationId: String)

    @Transaction
    suspend fun replaceForConversation(conversationId: String, items: List<ReceiptEntity>) {
        clear(conversationId)
        if (items.isNotEmpty()) upsert(items)
    }
}

@Dao
interface OutboxDao {
    @Query(
        "SELECT * FROM outbox WHERE state IN ('PENDING', 'FAILED') AND next_attempt_at_millis <= :nowMillis " +
            "ORDER BY client_created_at_millis ASC LIMIT :limit",
    )
    suspend fun ready(nowMillis: Long, limit: Int): List<OutboxEntity>

    @Query(
        "SELECT outbox.* FROM outbox INNER JOIN inbox " +
            "ON inbox.conversation_id = outbox.conversation_id " +
            "WHERE inbox.owner_id = :ownerId AND inbox.org_id = :orgId " +
            "AND outbox.state IN ('PENDING', 'FAILED') " +
            "AND outbox.next_attempt_at_millis <= :nowMillis " +
            "ORDER BY outbox.client_created_at_millis ASC LIMIT :limit",
    )
    suspend fun readyForOwner(
        ownerId: String,
        orgId: String,
        nowMillis: Long,
        limit: Int,
    ): List<OutboxEntity>

    @Query(
        "SELECT * FROM outbox WHERE conversation_id = :conversationId " +
            "AND operation_type = 'SET_REACTION'",
    )
    suspend fun reactionOperations(conversationId: String): List<OutboxEntity>

    @Query(
        "SELECT MIN(next_attempt_at_millis) FROM outbox " +
            "WHERE state IN ('PENDING', 'FAILED')",
    )
    suspend fun earliestOutstandingAtMillis(): Long?

    @Query(
        "SELECT MIN(outbox.next_attempt_at_millis) FROM outbox INNER JOIN inbox " +
            "ON inbox.conversation_id = outbox.conversation_id " +
            "WHERE inbox.owner_id = :ownerId AND inbox.org_id = :orgId " +
            "AND outbox.state IN ('PENDING', 'FAILED')",
    )
    suspend fun earliestOutstandingAtMillisForOwner(ownerId: String, orgId: String): Long?

    @Query("SELECT * FROM outbox WHERE operation_id = :operationId LIMIT 1")
    suspend fun get(operationId: String): OutboxEntity?

    @Upsert suspend fun upsert(item: OutboxEntity)

    @Query(
        "UPDATE outbox SET state = :state WHERE operation_id = :operationId " +
            "AND client_created_at_millis = :clientCreatedAtMillis",
    )
    suspend fun setState(operationId: String, clientCreatedAtMillis: Long, state: OutboxState): Int

    @Query("UPDATE outbox SET state = 'PENDING' WHERE state = 'PROCESSING'")
    suspend fun recoverInterrupted()

    @Query(
        "UPDATE outbox SET state = 'PENDING' WHERE state = 'PROCESSING' " +
            "AND EXISTS (SELECT 1 FROM inbox WHERE " +
            "inbox.conversation_id = outbox.conversation_id " +
            "AND inbox.owner_id = :ownerId AND inbox.org_id = :orgId)",
    )
    suspend fun recoverInterruptedForOwner(ownerId: String, orgId: String)

    @Query(
        "UPDATE outbox SET state = 'FAILED', attempt_count = attempt_count + 1, " +
            "next_attempt_at_millis = :nextAttemptAtMillis, last_error_code = :errorCode " +
            "WHERE operation_id = :operationId AND client_created_at_millis = :clientCreatedAtMillis",
    )
    suspend fun markFailed(
        operationId: String,
        clientCreatedAtMillis: Long,
        errorCode: String?,
        nextAttemptAtMillis: Long,
    )

    @Query("DELETE FROM outbox WHERE operation_id = :operationId AND client_created_at_millis = :clientCreatedAtMillis")
    suspend fun delete(operationId: String, clientCreatedAtMillis: Long)
    @Query("DELETE FROM outbox WHERE conversation_id = :conversationId")
    suspend fun clear(conversationId: String)
    @Query("DELETE FROM outbox") suspend fun clear()
}

@Dao
interface DraftDao {
    @Query("SELECT body FROM drafts WHERE conversation_id = :conversationId LIMIT 1")
    fun observeBody(conversationId: String): Flow<String?>

    @Upsert suspend fun upsert(item: DraftEntity)
    @Query("DELETE FROM drafts WHERE conversation_id = :conversationId") suspend fun delete(conversationId: String)
    @Query("DELETE FROM drafts") suspend fun clear()
}

@Dao
interface ConversationStateDao {
    @Query("SELECT * FROM conversation_state WHERE conversation_id = :conversationId LIMIT 1")
    suspend fun get(conversationId: String): ConversationStateEntity?
    @Upsert suspend fun upsert(item: ConversationStateEntity)
    @Query("DELETE FROM conversation_state WHERE conversation_id = :conversationId")
    suspend fun delete(conversationId: String)
    @Query("DELETE FROM conversation_state") suspend fun clear()
}

@Dao
interface SearchDao {
    @Query("SELECT rowid, * FROM search_fts WHERE search_fts MATCH :matchQuery LIMIT :limit")
    suspend fun search(matchQuery: String, limit: Int = 50): List<SearchFtsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun index(item: SearchFtsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun index(items: List<SearchFtsEntity>)

    @Query("DELETE FROM search_fts WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun remove(entityType: String, entityId: String)

    @Query("DELETE FROM search_fts WHERE conversation_id = :conversationId")
    suspend fun removeConversation(conversationId: String)

    @Query("DELETE FROM search_fts") suspend fun clear()
}
