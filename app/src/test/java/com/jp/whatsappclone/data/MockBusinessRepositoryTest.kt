package com.jp.whatsappclone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockBusinessRepositoryTest {

    @Test
    fun seededGroupHasMembersAdminsAndNamedMessages() {
        val state = MockBusinessRepository().appState.value
        val group = state.chats.first { it.id == "chat_team" }
        val members = state.groupMembers.filter { it.groupId == group.contact.id }

        assertTrue(group.contact.isGroup)
        assertTrue(members.size >= 10)
        assertTrue(members.any { it.contact.name == "You" && it.isAdmin })
        assertTrue(group.messages.any { it.direction == MessageDirection.Incoming && it.senderName != null })
    }
    @Test
    fun seedDataIsDeterministic() {
        val first = MockBusinessRepository().appState.value
        val second = MockBusinessRepository().appState.value

        assertEquals(first, second)
        assertEquals(7, first.chats.size)
        assertEquals("Mia Kapoor", first.chats.first().contact.name)
        assertEquals(23, first.chats.sumOf { it.unreadCount })
    }

    @Test
    fun textMessageIsTrimmedAndAppended() {
        val repository = MockBusinessRepository()
        val before = repository.appState.value.chats.first { it.id == "chat_mia" }.messages.size

        repository.dispatch(AppAction.SendText("chat_mia", "  Sent from test  "))

        val chat = repository.appState.value.chats.first { it.id == "chat_mia" }
        assertEquals(before + 1, chat.messages.size)
        assertEquals("Sent from test", chat.messages.last().body)
        assertEquals(DeliveryStatus.Sent, chat.messages.last().status)
        assertEquals("Sent from test", chat.preview)
    }

    @Test
    fun blankMessageIsIgnored() {
        val repository = MockBusinessRepository()
        val before = repository.appState.value.chats.first().messages

        repository.dispatch(AppAction.SendText("chat_mia", "   "))

        assertEquals(before, repository.appState.value.chats.first().messages)
    }

    @Test
    fun recordingPauseResumeAndSendTransitions() {
        val repository = MockBusinessRepository()

        repository.dispatch(AppAction.StartRecording)
        repository.dispatch(AppAction.TickRecording)
        repository.dispatch(AppAction.ToggleRecordingPause)
        repository.dispatch(AppAction.TickRecording)
        assertEquals(1, repository.appState.value.recording.seconds)

        repository.dispatch(AppAction.ToggleRecordingPause)
        repository.dispatch(AppAction.TickRecording)
        repository.dispatch(AppAction.SendRecording("chat_mia"))

        val state = repository.appState.value
        assertEquals(RecordingUi(), state.recording)
        assertEquals(2, state.chats.first { it.id == "chat_mia" }.messages.last().voiceSeconds)
    }

    @Test
    fun repliesReactionsDeletionAndAudioPathAreStored() {
        val repository = MockBusinessRepository()
        repository.dispatch(AppAction.SendText("chat_mia", "Reply sent", "Mia Kapoor", "Original message"))
        val reply = repository.appState.value.chats.first { it.id == "chat_mia" }.messages.last()
        assertEquals("Mia Kapoor", reply.replyToSender)
        assertEquals("Original message", reply.replyToBody)

        repository.dispatch(AppAction.SetMessageReaction("chat_mia", reply.id, "👍"))
        assertEquals("👍", repository.appState.value.chats.first { it.id == "chat_mia" }.messages.last().reaction)

        repository.dispatch(AppAction.DeleteMessage("chat_mia", reply.id))
        assertTrue(repository.appState.value.chats.first { it.id == "chat_mia" }.messages.last().deleted)

        repository.dispatch(AppAction.StartRecording)
        repository.dispatch(AppAction.SendRecording("chat_mia", "C:/mock/voice.m4a", 7))
        val voice = repository.appState.value.chats.first { it.id == "chat_mia" }.messages.last()
        assertEquals(7, voice.voiceSeconds)
        assertEquals("C:/mock/voice.m4a", voice.audioPath)
    }

    @Test
    fun searchFiltersApplyToMockData() {
        val repository = MockBusinessRepository()
        repository.dispatch(AppAction.ToggleSearchFilter("Unread"))
        assertTrue(repository.appState.value.filteredSearchChats().all { it.unreadCount > 0 })

        repository.dispatch(AppAction.ToggleSearchFilter("Unread"))
        repository.dispatch(AppAction.ToggleSearchFilter("Photos"))
        val photoResults = repository.appState.value.filteredSearchChats()
        assertEquals(listOf("chat_own"), photoResults.map { it.id })

        repository.dispatch(AppAction.ToggleSearchFilter("All"))
        assertTrue(repository.appState.value.searchFilters.isEmpty())
    }

    @Test
    fun homeFiltersSupportUnreadPinnedAndGroups() {
        val repository = MockBusinessRepository()

        repository.dispatch(AppAction.ToggleSearchFilter("Unread"))
        assertEquals(setOf("chat_dev", "chat_sara", "chat_team"), repository.appState.value.filteredHomeChats().map { it.id }.toSet())

        repository.dispatch(AppAction.ToggleSearchFilter("Favourites"))
        assertEquals(listOf("chat_dev"), repository.appState.value.filteredHomeChats().map { it.id })
    }

    @Test
    fun callAndNotificationActionsRemainLocal() {
        val repository = MockBusinessRepository()
        repository.dispatch(AppAction.StartCall("mia", video = true))
        repository.dispatch(AppAction.ToggleMessageMute)
        repository.dispatch(AppAction.ToggleStatusMute)

        val state = repository.appState.value
        assertEquals("mia", state.calls.first().contact.id)
        assertEquals(CallDirection.Outgoing, state.calls.first().direction)
        assertTrue(state.calls.first().video)
        assertTrue(state.messageNotificationsMuted)
        assertFalse(state.statusNotificationsMuted)
    }
}
