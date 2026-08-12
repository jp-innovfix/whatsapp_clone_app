package com.jp.whatsappclone.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockBusinessRepository {
    private val _appState = MutableStateFlow(seedState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private var nextMessageId = 100
    private var nextCallId = 100

    fun dispatch(action: AppAction) {
        when (action) {
            is AppAction.SelectTab -> _appState.update { it.copy(selectedTab = action.tab) }
            is AppAction.SetSearchQuery -> _appState.update { it.copy(searchQuery = action.query) }
            is AppAction.ToggleSearchFilter -> _appState.update { state ->
                val filters = state.searchFilters.toMutableSet().apply {
                    if (action.filter == "All") {
                        clear()
                    } else {
                        remove("All")
                        if (!add(action.filter)) remove(action.filter)
                    }
                }
                state.copy(searchFilters = filters)
            }
            is AppAction.SendText -> {
                val text = action.text.trim()
                if (text.isNotEmpty()) appendMessage(
                    action.threadId,
                    MessageUi(
                        id = "m${nextMessageId++}",
                        direction = MessageDirection.Outgoing,
                        body = text,
                        time = "3:36 am",
                        status = DeliveryStatus.Sent,
                        replyToSender = action.replyToSender,
                        replyToBody = action.replyToBody,
                    ),
                )
            }
            AppAction.StartRecording -> _appState.update { it.copy(recording = RecordingUi(active = true)) }
            AppAction.TickRecording -> _appState.update { state ->
                if (state.recording.active && !state.recording.paused) {
                    state.copy(recording = state.recording.copy(seconds = state.recording.seconds + 1))
                } else state
            }
            AppAction.ToggleRecordingPause -> _appState.update { state ->
                state.copy(recording = state.recording.copy(paused = !state.recording.paused))
            }
            AppAction.CancelRecording -> _appState.update { it.copy(recording = RecordingUi()) }
            is AppAction.SendRecording -> {
                val seconds = (action.durationSeconds ?: _appState.value.recording.seconds).coerceAtLeast(1)
                appendMessage(
                    action.threadId,
                    MessageUi(
                        id = "m${nextMessageId++}",
                        direction = MessageDirection.Outgoing,
                        time = "3:36 am",
                        status = DeliveryStatus.Sent,
                        voiceSeconds = seconds,
                        audioPath = action.audioPath,
                    ),
                )
                _appState.update { it.copy(recording = RecordingUi()) }
            }
            is AppAction.SetMessageReaction -> updateMessage(action.threadId, action.messageId) {
                it.copy(reaction = action.reaction)
            }
            is AppAction.DeleteMessage -> updateMessage(action.threadId, action.messageId) {
                it.copy(body = "", deleted = true, voiceSeconds = null, audioPath = null, reaction = null)
            }
            AppAction.DismissDraftAd -> _appState.update { it.copy(draftAdVisible = false) }
            AppAction.ToggleMessageMute -> _appState.update {
                it.copy(messageNotificationsMuted = !it.messageNotificationsMuted)
            }
            AppAction.ToggleStatusMute -> _appState.update {
                it.copy(statusNotificationsMuted = !it.statusNotificationsMuted)
            }
            is AppAction.StartCall -> _appState.update { state ->
                val contact = state.contacts.firstOrNull { it.id == action.contactId } ?: return@update state
                state.copy(
                    calls = listOf(
                        CallLogUi(
                            id = "call${nextCallId++}",
                            contact = contact,
                            direction = CallDirection.Outgoing,
                            whenLabel = "Today, 3:36 am",
                            video = action.video,
                        ),
                    ) + state.calls,
                )
            }
        }
    }

    private fun appendMessage(threadId: String, message: MessageUi) {
        _appState.update { state ->
            state.copy(chats = state.chats.map { thread ->
                if (thread.id == threadId) {
                    val preview = if (message.voiceSeconds != null) "Voice message" else message.body
                    thread.copy(messages = thread.messages + message, preview = preview, time = message.time)
                } else thread
            })
        }
    }

    private fun updateMessage(threadId: String, messageId: String, transform: (MessageUi) -> MessageUi) {
        _appState.update { state ->
            state.copy(chats = state.chats.map { thread ->
                if (thread.id == threadId) {
                    thread.copy(messages = thread.messages.map { message ->
                        if (message.id == messageId) transform(message) else message
                    })
                } else thread
            })
        }
    }

    private fun seedState(): AppState {
        val mia = ContactUi("mia", "Mia Kapoor", "MK", 0xFF66475EL, "last seen yesterday at 9:14 pm")
        val dev = ContactUi("dev", "Dev Product Lead", "DP", 0xFF234F74L, "online")
        val sara = ContactUi("sara", "Sara Operations", "SO", 0xFF6E443BL, "last seen today at 1:20 am")
        val team = ContactUi("team", "INNOVFIX", "I", 0xFF000000L, "Anirudh, Bala, Ikshitha, Kishore, You", true)
        val own = ContactUi("own", "Northstar Studio (You)", "NS", 0xFF31576EL, "Message yourself")
        val mona = ContactUi("mona", "Mona Friend", "MF", 0xFF5C3A66L)
        val rohan = ContactUi("rohan", "Rohan Ahmedabad", "RA", 0xFF704A2FL)
        val reena = ContactUi("reena", "Reena Creative", "RC", 0xFF395E48L)
        val aisha = ContactUi("aisha", "Aisha", "A", 0xFF68324CL, isSaved = false)
        val bala = ContactUi("bala", "Bala", "B", 0xFF455A64L, "Product designer")
        val kiran = ContactUi("kiran", "Kiran Support", "KS", 0xFF6A3F57L, "Available")
        val nantha = ContactUi("nantha", "Nantha Care", "NC", 0xFF075E54L, "Customer success")
        val anas = ContactUi("anas", "Anas Akbar Video Editor", "AV", 0xFF405C77L, "Editing today")
        val yuvan = ContactUi("yuvan", "Yuvanesh Ai", "YA", 0xFF6B533FL, "At work")
        val sindhiya = ContactUi("sindhiya", "Sindhiya Manager", "SM", 0xFF704456L, "Mai apni fav Hoon ❤️")
        val akshara = ContactUi("akshara", "Akshara", "A", 0xFF4C354CL, "Available")
        val ayush = ContactUi("ayush", "Ayush Co founder", "AC", 0xFF4B372FL, "In a meeting")
        val accountant = ContactUi("accountant", "Accountant Innovfix", "AI", 0xFF5B646AL, "Shoyab")
        val contacts = listOf(
            own, mia, dev, sara, team, mona, rohan, reena, aisha, bala, kiran,
            nantha, anas, yuvan, sindhiya, akshara, ayush, accountant,
        )

        val groupMembers = listOf(
            GroupMemberUi("team", own.copy(name = "You"), isAdmin = true, about = "CEO"),
            GroupMemberUi("team", akshara, isAdmin = true),
            GroupMemberUi("team", ayush, isAdmin = true, about = ayush.subtitle),
            GroupMemberUi("team", bala, isAdmin = true),
            GroupMemberUi("team", nantha, isAdmin = true),
            GroupMemberUi("team", yuvan, isAdmin = true),
            GroupMemberUi("team", accountant, about = accountant.subtitle),
            GroupMemberUi("team", sindhiya, about = sindhiya.subtitle),
            GroupMemberUi("team", anas, about = anas.subtitle),
            GroupMemberUi("team", mia, about = "Available"),
            GroupMemberUi("team", dev, about = "Product lead"),
            GroupMemberUi("team", sara, about = "Operations"),
            GroupMemberUi("team", rohan, about = "In a meeting"),
            GroupMemberUi("team", reena, about = "Designing something new"),
            GroupMemberUi("team", mona, about = "Hey there! I am using this prototype"),
            GroupMemberUi("team", kiran, about = kiran.subtitle),
        )

        val miaMessages = listOf(
            MessageUi("m1", MessageDirection.Outgoing, "Reimbursement", "12:09 am", DeliveryStatus.Read),
            MessageUi("m2", MessageDirection.Incoming, "", "12:09 am", voiceSeconds = 15),
            MessageUi("m3", MessageDirection.Incoming, "Could you send the updated ID card? 🌚", "12:10 am"),
            MessageUi("m4", MessageDirection.Outgoing, "", "12:11 am", DeliveryStatus.Read, voiceSeconds = 13),
            MessageUi("m5", MessageDirection.Incoming, "I need it today 🌚", "12:11 am"),
            MessageUi("m6", MessageDirection.Outgoing, "Try to get that", "12:12 am", DeliveryStatus.Read),
            MessageUi("m7", MessageDirection.Outgoing, "If you are good", "12:12 am", DeliveryStatus.Read),
            MessageUi("m8", MessageDirection.Outgoing, "Then you will", "12:12 am", DeliveryStatus.Read),
            MessageUi("m9", MessageDirection.Incoming, "Okay 🙂", "12:12 am"),
        )
        val teamMessages = listOf(
            MessageUi(
                "g1",
                MessageDirection.Incoming,
                "Please make sure the content is concise, professional, and ready for the client review.",
                "11:42 am",
                senderName = "Dev Product Lead",
            ),
            MessageUi("g2", MessageDirection.Outgoing, "Sure, I will update it today.", "11:46 am", DeliveryStatus.Read),
            MessageUi(
                "g3",
                MessageDirection.Incoming,
                "The visual direction looks good. Let’s keep the same spacing across every screen.",
                "11:51 am",
                senderName = "Reena Creative",
            ),
            MessageUi("g4", MessageDirection.Incoming, "Okay, I will check the final build.", "12:01 pm", senderName = "Akshara"),
        )
        val chats = listOf(
            ChatThreadUi("chat_mia", mia, "You deleted this message", "1:44 am", messages = miaMessages +
                MessageUi("m10", MessageDirection.Outgoing, time = "1:44 am", status = DeliveryStatus.Read, deleted = true)),
            ChatThreadUi("chat_dev", dev, "You reacted 👍 to “Looks good, please try again”", "1:33 am", unreadCount = 3, pinned = true),
            ChatThreadUi("chat_sara", sara, "Okay 🙂", "12:12 am", unreadCount = 2),
            ChatThreadUi("chat_team", team, "Akshara: Okay, I will check the final build", "12:01 pm", unreadCount = 18, muted = true, messages = teamMessages),
            ChatThreadUi("chat_own", own, "3 photos", "Yesterday", mediaLabel = "3 photos"),
            ChatThreadUi("chat_mona", mona, "😊", "Yesterday"),
            ChatThreadUi("chat_rohan", rohan, "Draft: invoice follow-up", "Sunday", draft = true),
        )
        val calls = listOf(
            CallLogUi("c1", rohan, CallDirection.Incoming, "Yesterday, 9:03 pm"),
            CallLogUi("c2", sara, CallDirection.Incoming, "Yesterday, 2:51 pm"),
            CallLogUi("c3", mia, CallDirection.Outgoing, "Yesterday, 2:14 pm"),
            CallLogUi("c4", reena, CallDirection.Outgoing, "9 August, 1:44 pm"),
            CallLogUi("c5", mona, CallDirection.Missed, "8 August, 11:54 pm", video = true),
            CallLogUi("c6", dev, CallDirection.Incoming, "8 August, 12:35 pm"),
        )
        val statuses = listOf(
            StatusUi("s0", own, "My status", 0xFF4E2D31L, 0xFF151C27L, mine = true),
            StatusUi("s1", mona, "Design notes", 0xFF536B65L, 0xFF222C31L),
            StatusUi("s2", reena, "Morning coast", 0xFF667B8DL, 0xFF25343DL),
            StatusUi("s3", rohan, "Founder meetup", 0xFF305C7AL, 0xFF7B402CL),
        )
        val channels = listOf(
            ChannelUi("ch1", "Startup Circle", "Community meetup this weekend", "Yesterday", 93, 0xFF315E42L),
            ChannelUi("ch2", "Design Team", "Photo", "Yesterday", 42, 0xFFB42162L),
            ChannelUi("ch3", "Morning Brief", "Good morning — today’s highlights", "Yesterday", 48, 0xFF4C2C72L),
            ChannelUi("ch4", "WhatsApp", "Sticker", "Yesterday", 6, 0xFF25D366L),
        )
        val metrics = listOf(
            BusinessMetricUi("Conversations\nstarted", "43", "chat", positive = true),
            BusinessMetricUi("Catalogue\nviews", "— —", "catalogue"),
            BusinessMetricUi("Status views", "— —", "status"),
        )
        val tools = listOf(
            ToolItemUi("Meta Verified", "Get a verified badge and other benefits", "verified"),
            ToolItemUi("Meta Business Agent", "Use an AI agent to respond to customers 24/7", "ai", accent = true),
            ToolItemUi("Catalogue", "Show products and services", "catalogue"),
            ToolItemUi("Advertise", "Create ads that reach new customers", "ads"),
            ToolItemUi("Business broadcasts", "Message multiple contacts at once", "broadcast"),
            ToolItemUi("Payments", "View history and manage info", "payments"),
            ToolItemUi("Lists", "Manage people and groups", "lists"),
            ToolItemUi("Greeting message", "Welcome new customers automatically", "greeting"),
            ToolItemUi("Away message", "Reply automatically when you’re away", "away"),
            ToolItemUi("Quick replies", "Reuse frequent messages", "quick"),
            ToolItemUi("Profile", "Manage address, hours, and websites", "profile"),
            ToolItemUi("Instagram & Facebook", "Connect to reach more customers", "link"),
        )
        return AppState(
            contacts = contacts,
            chats = chats,
            groupMembers = groupMembers,
            calls = calls,
            statuses = statuses,
            channels = channels,
            metrics = metrics,
            toolItems = tools,
        )
    }
}
