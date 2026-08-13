package com.jp.whatsappclone.data.repository

import androidx.paging.PagingData
import com.jp.whatsappclone.data.domain.InboxItem
import com.jp.whatsappclone.data.domain.Conversation
import com.jp.whatsappclone.data.domain.Member
import com.jp.whatsappclone.data.domain.LocalSearchMatch
import com.jp.whatsappclone.data.domain.Message
import com.jp.whatsappclone.data.domain.MessagePageRequest
import com.jp.whatsappclone.data.domain.MessagePageResult
import com.jp.whatsappclone.data.domain.Reaction
import com.jp.whatsappclone.data.domain.Receipt
import com.jp.whatsappclone.data.domain.SendTextCommand
import com.jp.whatsappclone.data.domain.SendAttachmentCommand
import com.jp.whatsappclone.data.domain.SessionState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSession(): Flow<SessionState>
    suspend fun ensureAnonymousSession(): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
    suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit>
}

interface DirectoryRepository {
    fun observeActiveMembers(): Flow<List<Member>>
}

interface ConversationRepository {
    fun observeInbox(): Flow<List<InboxItem>>
    fun observeConversation(conversationId: String): Flow<Conversation?>
    suspend fun getOrCreateDirectConversation(targetUserId: String): Result<String>
    suspend fun createGroup(title: String, memberIds: List<String>): Result<String>
    suspend fun setMuted(conversationId: String, muted: Boolean): Result<Unit>
    suspend fun setPinned(conversationId: String, pinned: Boolean): Result<Unit>
    suspend fun setArchived(conversationId: String, archived: Boolean): Result<Unit>
}

interface SearchRepository {
    suspend fun searchDownloaded(query: String, limit: Int = 100): List<LocalSearchMatch>
}

interface MessagingRepository {
    fun observeMessages(conversationId: String): Flow<PagingData<Message>>
    fun observeCachedMessages(conversationId: String): Flow<List<Message>>
    fun observeReactions(conversationId: String): Flow<List<Reaction>>
    fun observeReceipts(conversationId: String): Flow<List<Receipt>>
    suspend fun pageMessages(request: MessagePageRequest): Result<MessagePageResult>
    suspend fun sendText(command: SendTextCommand): Result<String>
    suspend fun sendAttachment(command: SendAttachmentCommand): Result<String>
    suspend fun forwardMessage(sourceConversationId: String, messageId: String, targetConversationId: String): Result<String>
    suspend fun prepareAttachment(messageId: String): Result<String>
    suspend fun retry(conversationId: String, messageId: String): Result<Unit>
    suspend fun reply(command: SendTextCommand): Result<String> = sendText(command)
    suspend fun setReaction(conversationId: String, messageId: String, reaction: String?): Result<Unit>
    suspend fun deleteForEveryone(conversationId: String, messageId: String): Result<Unit>
    suspend fun markRead(conversationId: String, throughMessageId: String): Result<Unit>
    suspend fun setTyping(conversationId: String, isTyping: Boolean): Result<Unit>
    fun observeTyping(conversationId: String): Flow<Set<String>>
    fun observeDraft(conversationId: String): Flow<String>
    suspend fun saveDraft(conversationId: String, body: String)
}
