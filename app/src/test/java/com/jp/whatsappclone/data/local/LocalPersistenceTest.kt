package com.jp.whatsappclone.data.local

import android.content.Context
import androidx.room.Room
import androidx.paging.PagingSource
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalPersistenceTest {
    private lateinit var database: InnovfixDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, InnovfixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun messageInsertIsIdempotentAndTimelineUsesServerThenClientTime() = runTest {
        val first = message(id = "0001", clientTime = 100)
        val second = message(id = "0002", clientTime = 50, serverTime = 200)
        val third = message(id = "0003", clientTime = 300)

        assertNotEquals(-1L, database.messageDao().insertIfAbsent(third))
        assertNotEquals(-1L, database.messageDao().insertIfAbsent(first))
        assertNotEquals(-1L, database.messageDao().insertIfAbsent(second))
        assertEquals(-1L, database.messageDao().insertIfAbsent(first.copy(body = "duplicate")))

        assertEquals(
            listOf("0001", "0002", "0003"),
            database.messageDao().observeAllAscending(CONVERSATION_ID).first().map { it.messageId },
        )
        assertEquals("body-0001", database.messageDao().get("0001")?.body)
    }

    @Test
    fun deletingMessageLeavesAnOrderedBodylessTombstone() = runTest {
        database.messageDao().insertIfAbsent(message(id = "0001", clientTime = 100))
        database.messageDao().insertIfAbsent(message(id = "0002", clientTime = 200))

        database.messageDao().markDeleted("0001", deletedAtMillis = 900)

        val tombstone = database.messageDao().get("0001")
        assertNotNull(tombstone)
        assertTrue(tombstone!!.deleted)
        assertEquals("", tombstone.body)
        assertEquals(900L, tombstone.deletedAtMillis)
        assertEquals(
            listOf("0001", "0002"),
            database.messageDao().observeAllAscending(CONVERSATION_ID).first().map { it.messageId },
        )
    }

    @Test
    fun pagingIsNewestFirstAndProjectsReplyTombstonesWithoutLoadingTheTimeline() = runTest {
        database.messageDao().insertIfAbsent(message(id = "original", clientTime = 100))
        database.messageDao().insertIfAbsent(
            message(id = "reply", clientTime = 200).copy(
                senderId = "recipient",
                replyToMessageId = "original",
            ),
        )
        database.messageDao().insertIfAbsent(message(id = "newest", clientTime = 300))

        val firstPage = database.messageDao().pagingSource(CONVERSATION_ID).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 2, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page<Int, MessagePageRow>

        assertEquals(listOf("newest", "reply"), firstPage.data.map { it.message.messageId })
        assertEquals("original", firstPage.data.last().replyMessageId)
        assertEquals(OWNER_ID, firstPage.data.last().replySenderId)
        assertEquals("body-original", firstPage.data.last().replyBody)
        assertFalse(firstPage.data.last().replyDeleted == true)

        database.messageDao().markDeleted("original", deletedAtMillis = 900)
        val refreshed = database.messageDao().pagingSource(CONVERSATION_ID).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 2, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page<Int, MessagePageRow>

        assertTrue(refreshed.data.last().replyDeleted == true)
        assertEquals("", refreshed.data.last().replyBody)
    }

    @Test
    fun inboxOrdersPinsFirstAndOwnerReplacementDoesNotEraseAnotherOwner() = runTest {
        val otherOwner = inbox(ownerId = "other", id = "other-chat", pinned = false, activity = 1)
        database.inboxDao().upsert(otherOwner)
        database.inboxDao().replaceForOwner(
            OWNER_ID,
            listOf(
                inbox(OWNER_ID, "recent", pinned = false, activity = 500),
                inbox(OWNER_ID, "old-pin", pinned = true, activity = 100),
                inbox(OWNER_ID, "new-pin", pinned = true, activity = 300),
            ),
        )

        assertEquals(
            listOf("new-pin", "old-pin", "recent"),
            database.inboxDao().observe(OWNER_ID).first().map { it.conversationId },
        )
        assertNotNull(database.inboxDao().get("other", "other-chat"))

        database.inboxDao().replaceForOwner(
            OWNER_ID,
            listOf(inbox(OWNER_ID, "replacement", pinned = false, activity = 10)),
        )

        assertNull(database.inboxDao().get(OWNER_ID, "recent"))
        assertNotNull(database.inboxDao().get(OWNER_ID, "replacement"))
        assertNotNull(database.inboxDao().get("other", "other-chat"))
    }

    @Test
    fun inboxPersistsArchivedPreferenceWithoutChangingOtherPreferences() = runTest {
        database.inboxDao().upsert(
            inbox(OWNER_ID, CONVERSATION_ID, pinned = true, activity = 10).copy(muted = true),
        )

        database.inboxDao().setArchived(OWNER_ID, CONVERSATION_ID, archived = true, updatedAtMillis = 20)

        val archived = requireNotNull(database.inboxDao().get(OWNER_ID, CONVERSATION_ID))
        assertTrue(archived.archived)
        assertTrue(archived.pinned)
        assertTrue(archived.muted)
        assertEquals(20L, archived.updatedAtMillis)
    }

    @Test
    fun outboxRecoversInterruptedWorkAndUsesTimestampAsAnAbaGuard() = runTest {
        val interrupted = outbox("interrupted", 10, OutboxState.PROCESSING)
        val deferred = outbox("deferred", 20, OutboxState.PENDING, nextAttempt = 1_000)
        val retryable = outbox("retryable", 30, OutboxState.FAILED, nextAttempt = 50)
        database.outboxDao().upsert(interrupted)
        database.outboxDao().upsert(deferred)
        database.outboxDao().upsert(retryable)

        database.outboxDao().recoverInterrupted()

        assertEquals(OutboxState.PENDING, database.outboxDao().get("interrupted")?.state)
        assertEquals(
            listOf("interrupted", "retryable"),
            database.outboxDao().ready(nowMillis = 100, limit = 10).map { it.operationId },
        )

        database.outboxDao().setState("interrupted", clientCreatedAtMillis = 9, OutboxState.PROCESSING)
        assertEquals(OutboxState.PENDING, database.outboxDao().get("interrupted")?.state)
        database.outboxDao().setState("interrupted", clientCreatedAtMillis = 10, OutboxState.PROCESSING)
        assertEquals(OutboxState.PROCESSING, database.outboxDao().get("interrupted")?.state)

        database.outboxDao().markFailed("interrupted", 9, "wrong-generation", 500)
        assertEquals(0, database.outboxDao().get("interrupted")?.attemptCount)
        database.outboxDao().markFailed("interrupted", 10, "network", 500)
        assertEquals(1, database.outboxDao().get("interrupted")?.attemptCount)
        assertEquals("network", database.outboxDao().get("interrupted")?.lastErrorCode)

        database.outboxDao().delete("interrupted", clientCreatedAtMillis = 9)
        assertNotNull(database.outboxDao().get("interrupted"))
        database.outboxDao().delete("interrupted", clientCreatedAtMillis = 10)
        assertNull(database.outboxDao().get("interrupted"))
    }

    @Test
    fun outboxSelectionRequiresTheCurrentOwnerAndOrganizationInbox() = runTest {
        database.outboxDao().upsert(outbox("authorized", 10, OutboxState.PENDING))

        assertTrue(
            database.outboxDao().readyForOwner(OWNER_ID, "innovfix", 100, 10).isEmpty(),
        )
        database.inboxDao().upsert(inbox(OWNER_ID, CONVERSATION_ID, false, 10))

        assertEquals(
            listOf("authorized"),
            database.outboxDao().readyForOwner(OWNER_ID, "innovfix", 100, 10)
                .map { it.operationId },
        )
        assertTrue(database.outboxDao().readyForOwner("other", "innovfix", 100, 10).isEmpty())
        assertTrue(database.outboxDao().readyForOwner(OWNER_ID, "other-org", 100, 10).isEmpty())
        assertEquals(
            0,
            database.outboxDao().setState("authorized", 9, OutboxState.PROCESSING),
        )
        assertEquals(
            1,
            database.outboxDao().setState("authorized", 10, OutboxState.PROCESSING),
        )
    }

    @Test
    fun clearUserDataRemovesEveryUserScopedTable() = runTest {
        database.prepareForOwner(OWNER_ID, "innovfix")
        database.storeValidatedSelfProfile(OWNER_ID, "innovfix", member(), validatedAtMillis = 10)
        database.memberDao().upsert(member())
        database.conversationDao().upsert(conversation())
        database.inboxDao().upsert(inbox(OWNER_ID, CONVERSATION_ID, false, 10))
        database.messageDao().insertIfAbsent(message("message", 10))
        database.reactionDao().upsert(
            ReactionEntity(CONVERSATION_ID, "message", OWNER_ID, "👍", 10, 10),
        )
        database.receiptDao().upsert(
            listOf(ReceiptEntity(CONVERSATION_ID, OWNER_ID, 10, 10, 10)),
        )
        database.outboxDao().upsert(outbox("operation", 10, OutboxState.PENDING))
        database.draftDao().upsert(DraftEntity(CONVERSATION_ID, OWNER_ID, "secret draft", 10))
        database.conversationStateDao().upsert(ConversationStateEntity(CONVERSATION_ID))
        database.searchDao().index(SearchFtsEntity(1, "MESSAGE", "message", CONVERSATION_ID, "secret body"))

        database.clearUserData()

        assertNull(database.localAccountDao().get())
        assertNull(database.memberDao().get(OWNER_ID))
        assertNull(database.conversationDao().get(CONVERSATION_ID))
        assertTrue(database.inboxDao().observe(OWNER_ID).first().isEmpty())
        assertNull(database.messageDao().get("message"))
        assertTrue(database.reactionDao().observe(CONVERSATION_ID).first().isEmpty())
        assertTrue(database.receiptDao().observe(CONVERSATION_ID).first().isEmpty())
        assertTrue(database.outboxDao().ready(Long.MAX_VALUE, 10).isEmpty())
        assertNull(database.draftDao().observeBody(CONVERSATION_ID).first())
        assertNull(database.conversationStateDao().get(CONVERSATION_ID))
        assertTrue(database.searchDao().search("secret*").isEmpty())
    }

    @Test
    fun downloadedMembersChatsAndMessagesAreSearchableByPrefix() = runTest {
        val member = member().copy(displayName = "Akshara Nair")
        val inbox = inbox(OWNER_ID, CONVERSATION_ID, false, 10).copy(
            title = "Design Team",
            lastMessagePreview = "Sprint review",
        )
        val message = message("message", 20).copy(body = "Project Atlas is ready")
        database.searchDao().index(
            listOf(
                SearchIndex.member(member),
                SearchIndex.inbox(inbox),
                SearchIndex.message(message),
            ),
        )

        assertEquals(
            listOf(SearchIndex.MEMBER),
            database.searchDao().search(requireNotNull(SearchIndex.prefixQuery("aksh"))).map { it.entityType },
        )
        assertEquals(
            CONVERSATION_ID,
            database.searchDao().search(requireNotNull(SearchIndex.prefixQuery("atlas"))).single().conversationId,
        )
        assertEquals(
            SearchIndex.INBOX,
            database.searchDao().search(requireNotNull(SearchIndex.prefixQuery("sprint rev"))).single().entityType,
        )

        database.searchDao().remove(SearchIndex.MESSAGE, message.messageId)
        assertTrue(database.searchDao().search(requireNotNull(SearchIndex.prefixQuery("atlas"))).isEmpty())
    }

    @Test
    fun cachedSelfProfileRequiresExactValidatedDatabaseOwner() = runTest {
        assertFalse(database.prepareForOwner(OWNER_ID, "innovfix"))
        database.memberDao().upsert(member())

        assertNull(database.cachedActiveSelfProfile(OWNER_ID, "innovfix"))
        assertTrue(
            database.storeValidatedSelfProfile(
                OWNER_ID,
                "innovfix",
                member(),
                validatedAtMillis = 100,
            ),
        )
        assertEquals(OWNER_ID, database.cachedActiveSelfProfile(OWNER_ID, "innovfix")?.memberId)
        assertNull(database.cachedActiveSelfProfile("another-user", "innovfix"))
        assertNull(database.cachedActiveSelfProfile(OWNER_ID, "another-org"))
    }

    @Test
    fun ownerSwitchAtomicallyPurgesPriorAccountAndRejectsLateValidation() = runTest {
        database.prepareForOwner(OWNER_ID, "innovfix")
        assertTrue(database.storeValidatedSelfProfile(OWNER_ID, "innovfix", member(), 100))
        database.messageDao().insertIfAbsent(message("private-message", 10))
        database.draftDao().upsert(DraftEntity(CONVERSATION_ID, OWNER_ID, "private draft", 10))

        assertFalse(database.prepareForOwner("new-owner", "innovfix"))

        assertEquals("new-owner", database.localAccountDao().get()?.ownerUid)
        assertNull(database.messageDao().get("private-message"))
        assertNull(database.draftDao().observeBody(CONVERSATION_ID).first())
        assertNull(database.memberDao().get(OWNER_ID))
        assertFalse(
            database.storeValidatedSelfProfile(OWNER_ID, "innovfix", member(), 200),
        )
        assertEquals("new-owner", database.localAccountDao().get()?.ownerUid)
    }

    @Test
    fun inactiveOrMismatchedSelfProfileCannotBecomeOfflineCredential() = runTest {
        database.prepareForOwner(OWNER_ID, "innovfix")

        runCatching {
            database.storeValidatedSelfProfile(
                OWNER_ID,
                "innovfix",
                member().copy(active = false),
            )
        }.onSuccess { throw AssertionError("Inactive profile was accepted") }
        runCatching {
            database.storeValidatedSelfProfile(
                OWNER_ID,
                "innovfix",
                member().copy(memberId = "another-user"),
            )
        }.onSuccess { throw AssertionError("Mismatched profile was accepted") }

        assertNull(database.cachedActiveSelfProfile(OWNER_ID, "innovfix"))
    }

    private fun message(
        id: String,
        clientTime: Long,
        serverTime: Long? = null,
    ) = MessageEntity(
        messageId = id,
        conversationId = CONVERSATION_ID,
        senderId = OWNER_ID,
        body = "body-$id",
        clientCreatedAtMillis = clientTime,
        serverCreatedAtMillis = serverTime,
        localStatus = "PENDING",
    )

    private fun outbox(
        id: String,
        createdAt: Long,
        state: OutboxState,
        nextAttempt: Long = 0,
    ) = OutboxEntity(
        operationId = id,
        operationType = OutboxOperation.SEND_MESSAGE,
        conversationId = CONVERSATION_ID,
        messageId = "message-$id",
        clientCreatedAtMillis = createdAt,
        state = state,
        nextAttemptAtMillis = nextAttempt,
    )

    private fun inbox(ownerId: String, id: String, pinned: Boolean, activity: Long) = InboxEntity(
        ownerId = ownerId,
        conversationId = id,
        orgId = "innovfix",
        type = "DIRECT",
        title = id,
        lastActivityAtMillis = activity,
        pinned = pinned,
        updatedAtMillis = activity,
    )

    private fun member() = MemberEntity(
        memberId = OWNER_ID,
        orgId = "innovfix",
        email = "owner@innovfix.test",
        displayName = "Owner",
        role = "MEMBER",
        active = true,
        updatedAtMillis = 10,
    )

    private fun conversation() = ConversationEntity(
        conversationId = CONVERSATION_ID,
        orgId = "innovfix",
        type = "DIRECT",
        memberIds = listOf(OWNER_ID, "recipient"),
        createdBy = OWNER_ID,
        createdAtMillis = 10,
        updatedAtMillis = 10,
    )

    private companion object {
        const val OWNER_ID = "owner"
        const val CONVERSATION_ID = "conversation"
    }
}
