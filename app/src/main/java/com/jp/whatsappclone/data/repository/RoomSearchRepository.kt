package com.jp.whatsappclone.data.repository

import androidx.room.withTransaction
import com.jp.whatsappclone.data.domain.LocalSearchMatch
import com.jp.whatsappclone.data.domain.LocalSearchEntityType
import com.jp.whatsappclone.data.local.InboxDao
import com.jp.whatsappclone.data.local.InnovfixDatabase
import com.jp.whatsappclone.data.local.MemberDao
import com.jp.whatsappclone.data.local.MessageDao
import com.jp.whatsappclone.data.local.SearchDao
import com.jp.whatsappclone.data.local.SearchIndex
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RoomSearchRepository @Inject constructor(
    private val database: InnovfixDatabase,
    private val memberDao: MemberDao,
    private val inboxDao: InboxDao,
    private val messageDao: MessageDao,
    private val searchDao: SearchDao,
) : SearchRepository {
    private val backfillMutex = Mutex()
    @Volatile private var backfillComplete = false

    override suspend fun searchDownloaded(query: String, limit: Int): List<LocalSearchMatch> {
        val matchQuery = SearchIndex.prefixQuery(query) ?: return emptyList()
        require(limit in 1..250) { "Search limit must be between 1 and 250" }
        ensureDownloadedDataIsIndexed()
        return searchDao.search(matchQuery, limit).mapNotNull { item ->
            val type = runCatching { LocalSearchEntityType.valueOf(item.entityType) }.getOrNull()
                ?: return@mapNotNull null
            LocalSearchMatch(type, item.entityId, item.conversationId)
        }
    }

    /** Backfills caches created before FTS indexing was added; later writes stay incremental. */
    private suspend fun ensureDownloadedDataIsIndexed() = backfillMutex.withLock {
        if (backfillComplete) return@withLock
        database.withTransaction {
            val entries = buildList {
                addAll(memberDao.active().map(SearchIndex::member))
                addAll(inboxDao.all().map(SearchIndex::inbox))
                addAll(messageDao.searchable().map(SearchIndex::message))
            }
            if (entries.isNotEmpty()) searchDao.index(entries)
        }
        backfillComplete = true
    }
}
