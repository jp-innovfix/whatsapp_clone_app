package com.jp.whatsappclone.data.local

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object SearchIndex {
    const val MEMBER = "MEMBER"
    const val INBOX = "INBOX"
    const val MESSAGE = "MESSAGE"

    fun member(entity: MemberEntity): SearchFtsEntity = SearchFtsEntity(
        rowId = stableRowId(MEMBER, entity.memberId),
        entityType = MEMBER,
        entityId = entity.memberId,
        conversationId = null,
        searchableText = "${entity.displayName} ${entity.email}",
    )

    fun inbox(entity: InboxEntity): SearchFtsEntity = SearchFtsEntity(
        rowId = stableRowId(INBOX, "${entity.ownerId}:${entity.conversationId}"),
        entityType = INBOX,
        entityId = entity.conversationId,
        conversationId = entity.conversationId,
        searchableText = "${entity.title} ${entity.lastMessagePreview}",
    )

    fun message(entity: MessageEntity): SearchFtsEntity = SearchFtsEntity(
        rowId = stableRowId(MESSAGE, entity.messageId),
        entityType = MESSAGE,
        entityId = entity.messageId,
        conversationId = entity.conversationId,
        searchableText = entity.body,
    )

    /** Creates an FTS prefix query without allowing user text to become FTS syntax. */
    fun prefixQuery(input: String): String? {
        val tokens = TOKEN.findAll(input)
            .map { it.value.lowercase() }
            .take(MAX_QUERY_TOKENS)
            .toList()
        if (tokens.isEmpty()) return null
        // Tokens contain only Unicode letters/digits, so they cannot inject FTS operators.
        // FTS4 recognizes the prefix marker only when it is part of the bare term.
        // Adjacent FTS4 terms are an implicit AND. This syntax is supported by Android's
        // bundled SQLite FTS4 implementation across our full minSdk range.
        return tokens.joinToString(" ") { token -> "$token*" }
    }

    internal fun stableRowId(entityType: String, entityId: String): Long {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$entityType:$entityId".toByteArray(StandardCharsets.UTF_8))
        val value = ByteBuffer.wrap(digest, 0, Long.SIZE_BYTES).long and Long.MAX_VALUE
        return value.takeUnless { it == 0L } ?: 1L
    }

    private val TOKEN = Regex("[\\p{L}\\p{N}]+")
    private const val MAX_QUERY_TOKENS = 8
}
