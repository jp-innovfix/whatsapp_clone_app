package com.jp.whatsappclone.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LocalAccountEntity::class,
        MemberEntity::class,
        ConversationEntity::class,
        InboxEntity::class,
        MessageEntity::class,
        ReactionEntity::class,
        ReceiptEntity::class,
        OutboxEntity::class,
        DraftEntity::class,
        ConversationStateEntity::class,
        SearchFtsEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class InnovfixDatabase : RoomDatabase() {
    abstract fun localAccountDao(): LocalAccountDao
    abstract fun memberDao(): MemberDao
    abstract fun conversationDao(): ConversationDao
    abstract fun inboxDao(): InboxDao
    abstract fun messageDao(): MessageDao
    abstract fun reactionDao(): ReactionDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun outboxDao(): OutboxDao
    abstract fun draftDao(): DraftDao
    abstract fun conversationStateDao(): ConversationStateDao
    abstract fun searchDao(): SearchDao

    suspend fun clearUserData() = withTransaction {
        clearUserScopedTables()
        localAccountDao().clear()
    }

    /**
     * Establishes the account allowed to read this database. A missing owner is treated as
     * untrusted legacy state, and a changed owner atomically purges all prior account data.
     *
     * @return true only when the database was already owned by this exact UID and organization.
     */
    suspend fun prepareForOwner(ownerUid: String, orgId: String): Boolean = withTransaction {
        require(ownerUid.isNotBlank()) { "Owner UID is required" }
        require(orgId.isNotBlank()) { "Organization ID is required" }
        val current = localAccountDao().get()
        if (current?.ownerUid == ownerUid && current.orgId == orgId) {
            true
        } else {
            clearUserScopedTables()
            localAccountDao().clear()
            localAccountDao().upsert(LocalAccountEntity(ownerUid = ownerUid, orgId = orgId))
            false
        }
    }

    /**
     * Stores the remotely validated self profile only if this database still belongs to the
     * expected account. This refusal prevents a late callback for account A from reclaiming a
     * database that an auth transition has already assigned to account B.
     */
    suspend fun storeValidatedSelfProfile(
        ownerUid: String,
        orgId: String,
        member: MemberEntity,
        validatedAtMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        require(member.memberId == ownerUid) { "Self profile UID does not match the database owner" }
        require(member.orgId == orgId) { "Self profile organization does not match the database owner" }
        require(member.active) { "An inactive self profile cannot be cached as validated" }
        return withTransaction {
            val current = localAccountDao().get()
            if (current?.ownerUid != ownerUid || current.orgId != orgId) {
                return@withTransaction false
            }
            memberDao().upsert(member)
            localAccountDao().upsert(
                LocalAccountEntity(
                    ownerUid = ownerUid,
                    orgId = orgId,
                    selfProfileValidatedAtMillis = validatedAtMillis,
                ),
            )
            true
        }
    }

    /** Returns only a previously remote-validated, still-active profile for the exact owner. */
    suspend fun cachedActiveSelfProfile(ownerUid: String, orgId: String): MemberEntity? = withTransaction {
        val account = localAccountDao().get()
        if (
            account?.ownerUid != ownerUid ||
            account.orgId != orgId ||
            account.selfProfileValidatedAtMillis == null
        ) {
            return@withTransaction null
        }
        memberDao().get(ownerUid)?.takeIf { member ->
            member.memberId == ownerUid && member.orgId == orgId && member.active
        }
    }

    suspend fun isOwnedBy(ownerUid: String, orgId: String): Boolean = withTransaction {
        localAccountDao().get()?.let { account ->
            account.ownerUid == ownerUid && account.orgId == orgId
        } == true
    }

    private suspend fun clearUserScopedTables() {
        searchDao().clear()
        outboxDao().clear()
        reactionDao().clear()
        receiptDao().clear()
        messageDao().clear()
        draftDao().clear()
        conversationStateDao().clear()
        inboxDao().clear()
        conversationDao().clear()
        memberDao().clear()
    }

    suspend fun purgeConversation(ownerId: String, conversationId: String) = withTransaction {
        searchDao().removeConversation(conversationId)
        outboxDao().clear(conversationId)
        reactionDao().clear(conversationId)
        receiptDao().clear(conversationId)
        messageDao().clear(conversationId)
        draftDao().delete(conversationId)
        conversationStateDao().delete(conversationId)
        inboxDao().delete(ownerId, conversationId)
        conversationDao().delete(conversationId)
    }

    companion object {
        const val FILE_NAME = "innovfix-messaging.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_account` (
                        `slot` INTEGER NOT NULL,
                        `owner_uid` TEXT NOT NULL,
                        `org_id` TEXT NOT NULL,
                        `self_profile_validated_at_millis` INTEGER,
                        PRIMARY KEY(`slot`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `inbox` ADD COLUMN `archived` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'TEXT'")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `storage_path` TEXT")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `file_name` TEXT")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `mime_type` TEXT")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `size_bytes` INTEGER")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `duration_millis` INTEGER")
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `local_media_path` TEXT")
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'TEXT'")
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `storage_path` TEXT")
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `file_name` TEXT")
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `mime_type` TEXT")
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `size_bytes` INTEGER")
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `duration_millis` INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `outbox` ADD COLUMN `local_media_path` TEXT")
            }
        }
    }
}
