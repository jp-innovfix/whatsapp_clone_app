package com.jp.whatsappclone.data.mapper

import com.jp.whatsappclone.data.domain.ConversationType
import com.jp.whatsappclone.data.domain.MemberRole
import com.jp.whatsappclone.data.domain.MessageDeliveryStatus
import com.jp.whatsappclone.data.domain.MessageKind
import com.jp.whatsappclone.data.domain.NotificationPreference
import com.jp.whatsappclone.data.local.InboxEntity
import com.jp.whatsappclone.data.local.MemberEntity
import com.jp.whatsappclone.data.local.MessageEntity
import com.jp.whatsappclone.data.local.MessagePageRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingMappersTest {
    @Test
    fun messageMapperPreservesEveryLocalDeliveryState() {
        MessageDeliveryStatus.entries.forEach { expected ->
            val mapped = message(localStatus = expected.name).toDomain()

            assertEquals(expected, mapped.status)
        }
    }

    @Test
    fun messageMapperPreservesTombstoneAndReplyMetadata() {
        val mapped = message(localStatus = "READ").copy(
            body = "",
            deleted = true,
            deletedAtMillis = 500,
            replyToMessageId = "original",
        ).toDomain()

        assertTrue(mapped.deleted)
        assertEquals("", mapped.body)
        assertEquals("original", mapped.replyToMessageId)
        assertEquals(MessageDeliveryStatus.READ, mapped.status)
        assertNull(mapped.failureReason)
    }

    @Test
    fun pageProjectionKeepsReplyContentTransientAndHonorsOriginalTombstone() {
        val projected = MessagePageRow(
            message = message(localStatus = "SENT").copy(replyToMessageId = "original"),
            replyMessageId = "original",
            replySenderId = "original-sender",
            replyBody = "",
            replyDeleted = true,
        ).toDomain()

        assertEquals("original-sender", projected.replySenderId)
        assertEquals("", projected.replyBody)
        assertTrue(projected.replyDeleted)
    }

    @Test
    fun messageMapperPreservesAttachmentMetadataAndLocalCache() {
        val mapped = message(localStatus = "SENT").copy(
            body = "Voice message",
            kind = MessageKind.AUDIO.name,
            storagePath = "organizations/innovfix/conversations/conversation/messages/message/voice.m4a",
            fileName = "voice.m4a",
            mimeType = "audio/mp4",
            sizeBytes = 4_096,
            durationMillis = 12_500,
            localMediaPath = "file:///cache/voice.m4a",
        ).toDomain()

        assertEquals(MessageKind.AUDIO, mapped.kind)
        assertEquals("voice.m4a", mapped.fileName)
        assertEquals("audio/mp4", mapped.mimeType)
        assertEquals(4_096L, mapped.sizeBytes)
        assertEquals(12_500L, mapped.durationMillis)
        assertEquals("file:///cache/voice.m4a", mapped.localMediaPath)
    }

    @Test
    fun unknownPersistedEnumsFallBackToSafeDomainValues() {
        val member = MemberEntity(
            memberId = "member",
            orgId = "innovfix",
            email = "member@innovfix.test",
            displayName = "Member",
            role = "FUTURE_ROLE",
            active = true,
            notificationPreference = "FUTURE_PREFERENCE",
            updatedAtMillis = 10,
        ).toDomain()
        val inbox = InboxEntity(
            ownerId = "member",
            conversationId = "conversation",
            orgId = "innovfix",
            type = "FUTURE_TYPE",
            title = "Conversation",
            lastMessageSenderId = "sender",
            updatedAtMillis = 10,
        ).toDomain()
        val message = message(localStatus = "FUTURE_STATUS").toDomain()

        assertEquals(MemberRole.MEMBER, member.role)
        assertEquals(NotificationPreference.ALL, member.notificationPreference)
        assertEquals(ConversationType.DIRECT, inbox.type)
        assertEquals("sender", inbox.lastMessageSenderId)
        assertEquals(MessageDeliveryStatus.SENT, message.status)
    }

    private fun message(localStatus: String) = MessageEntity(
        messageId = "message",
        conversationId = "conversation",
        senderId = "sender",
        body = "hello",
        clientCreatedAtMillis = 100,
        serverCreatedAtMillis = 200,
        localStatus = localStatus,
    )
}
