package com.jp.whatsappclone.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Forward
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jp.whatsappclone.data.AppAction
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.data.ChatThreadUi
import com.jp.whatsappclone.data.GroupMemberUi
import com.jp.whatsappclone.data.MessageDirection
import com.jp.whatsappclone.data.MessageUi
import com.jp.whatsappclone.ui.ActionWhite
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.AttachmentGreen
import com.jp.whatsappclone.ui.BrandGreen
import com.jp.whatsappclone.ui.ComponentSurface
import com.jp.whatsappclone.ui.DestructiveRed
import com.jp.whatsappclone.ui.ElevatedSurface
import com.jp.whatsappclone.ui.IncomingBubble
import com.jp.whatsappclone.ui.OutgoingBubble
import com.jp.whatsappclone.ui.PrimaryText
import com.jp.whatsappclone.ui.ReadBlue
import com.jp.whatsappclone.ui.SecondaryText
import com.jp.whatsappclone.ui.components.Avatar
import com.jp.whatsappclone.ui.components.DeliveryIcon
import com.jp.whatsappclone.ui.components.DoodleBackground
import com.jp.whatsappclone.ui.components.VoiceWaveform
import com.jp.whatsappclone.ui.audio.VoicePlaybackController
import com.jp.whatsappclone.ui.audio.VoiceRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ChatScreen(
    threadId: String,
    state: AppState,
    dispatch: (AppAction) -> Unit,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
    onInfo: () -> Unit = {},
    initialAttachmentSheet: Boolean = false,
    initialComposerText: String = "",
) {
    val thread = state.chats.firstOrNull { it.id == threadId } ?: state.chats.first()
    var attachmentSheet by rememberSaveable { mutableStateOf(initialAttachmentSheet) }
    var draft by rememberSaveable(threadId) { mutableStateOf(initialComposerText) }
    var emojiPanelOpen by rememberSaveable { mutableStateOf(false) }
    var replyingToId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val recorder = remember { VoiceRecorder(context) }
    val playback = remember { VoicePlaybackController() }
    val replyingTo = thread.messages.firstOrNull { it.id == replyingToId }
    val selectedMessage = thread.messages.firstOrNull { it.id == selectedMessageId }
    val groupMembers = state.groupMembers.filter { it.groupId == thread.contact.id }
    val mentionQuery = if (thread.contact.isGroup && !emojiPanelOpen) draft.activeMentionQuery() else null
    val mentionMembers = if (mentionQuery != null) {
        val priority = listOf("Sindhiya Manager", "Accountant Innovfix", "Akshara", "Anas Akbar Video Editor")
        groupMembers
            .filter { it.contact.name != "You" && it.contact.name.contains(mentionQuery, ignoreCase = true) }
            .sortedBy { member -> priority.indexOf(member.contact.name).let { if (it < 0) Int.MAX_VALUE else it } }
            .take(6)
    } else {
        emptyList()
    }

    val beginRecording = {
        focusManager.clearFocus()
        keyboard?.hide()
        emojiPanelOpen = false
        if (recorder.start()) {
            dispatch(AppAction.StartRecording)
        } else {
            showMessage("Unable to start the microphone")
        }
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginRecording() else showMessage("Microphone permission is required to record voice messages")
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            playback.release()
        }
    }

    LaunchedEffect(thread.messages.size) {
        if (thread.messages.isNotEmpty()) listState.animateScrollToItem(thread.messages.lastIndex + 1)
    }
    LaunchedEffect(state.recording.active, state.recording.paused) {
        while (state.recording.active && !state.recording.paused) {
            delay(1000)
            dispatch(AppAction.TickRecording)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            if (selectedMessage != null) {
                MessageSelectionTopBar(
                    onBack = { selectedMessageId = null },
                    onReply = {
                        replyingToId = selectedMessage.id
                        selectedMessageId = null
                        emojiPanelOpen = false
                        scope.launch {
                            delay(50)
                            focusRequester.requestFocus()
                            keyboard?.show()
                        }
                    },
                    onDelete = {
                        dispatch(AppAction.DeleteMessage(thread.id, selectedMessage.id))
                        selectedMessageId = null
                    },
                    onCopy = {
                        clipboard.setText(AnnotatedString(selectedMessage.body.ifBlank { "Voice message" }))
                        selectedMessageId = null
                        showMessage("Message copied")
                    },
                    onForward = { showMessage("Forward contact picker is mocked") },
                    onMore = { showMessage("Message options are mocked") },
                )
            } else {
                ChatTopBar(
                    thread = thread,
                    onBack = onBack,
                    onInfo = onInfo,
                    onVideo = {
                        dispatch(AppAction.StartCall(thread.contact.id, true))
                        showMessage("Mock video call started")
                    },
                    onCall = {
                        dispatch(AppAction.StartCall(thread.contact.id))
                        showMessage("Mock call started")
                    },
                    onMore = { showMessage("Conversation options are mocked") },
                )
            }
        },
        bottomBar = {
            if (state.recording.active) {
                RecordingPanel(
                    seconds = state.recording.seconds,
                    paused = state.recording.paused,
                    onDelete = {
                        recorder.cancel()
                        dispatch(AppAction.CancelRecording)
                    },
                    onPause = {
                        val changed = if (state.recording.paused) recorder.resume() else recorder.pause()
                        if (changed) dispatch(AppAction.ToggleRecordingPause)
                    },
                    onSend = {
                        val path = recorder.stop(keepFile = true)
                        dispatch(AppAction.SendRecording(thread.id, path, state.recording.seconds))
                    },
                )
            } else {
                Column(Modifier.imePadding()) {
                    if (mentionQuery != null) {
                        GroupMentionPicker(
                            members = mentionMembers,
                            includeAll = "all".startsWith(mentionQuery, ignoreCase = true) || mentionQuery.isBlank(),
                            onSelect = { mention ->
                                draft = draft.replaceActiveMention(mention)
                                scope.launch {
                                    focusRequester.requestFocus()
                                    keyboard?.show()
                                }
                            },
                        )
                    }
                    MessageComposer(
                        value = draft,
                        onValueChange = { draft = it },
                        replyingTo = replyingTo,
                        contactName = thread.contact.name,
                        onCancelReply = { replyingToId = null },
                        emojiPanelOpen = emojiPanelOpen,
                        focusRequester = focusRequester,
                        addNavigationPadding = !emojiPanelOpen,
                        onFocused = { emojiPanelOpen = false },
                        onEmojiToggle = {
                            if (emojiPanelOpen) {
                                emojiPanelOpen = false
                                scope.launch {
                                    delay(50)
                                    focusRequester.requestFocus()
                                    keyboard?.show()
                                }
                            } else {
                                focusManager.clearFocus()
                                keyboard?.hide()
                                emojiPanelOpen = true
                            }
                        },
                        onSend = {
                            if (draft.isNotBlank()) {
                                dispatch(
                                    AppAction.SendText(
                                        threadId = thread.id,
                                        text = draft,
                                        replyToSender = replyingTo?.let { if (it.direction == MessageDirection.Outgoing) "You" else thread.contact.name },
                                        replyToBody = replyingTo?.body?.ifBlank { "Voice message" },
                                    ),
                                )
                                draft = ""
                                replyingToId = null
                            }
                        },
                        onAttachment = { attachmentSheet = true },
                        onRecord = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                beginRecording()
                            } else {
                                microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onCamera = { showMessage("Camera is mocked") },
                    )
                    if (emojiPanelOpen) {
                        EmojiPicker(
                            onEmoji = { draft += it },
                            onBackspace = { draft = draft.removeLastCodePoint() },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            DoodleBackground()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Today",
                            color = SecondaryText,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(ElevatedSurface).padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
                items(thread.messages, key = { it.id }) { message ->
                    val voiceContact = if (message.direction == MessageDirection.Outgoing) {
                        state.contacts.firstOrNull { it.id == "own" } ?: thread.contact
                    } else {
                        thread.contact
                    }
                    MessageBubble(
                        message = message,
                        voiceContact = voiceContact,
                        showSender = thread.contact.isGroup,
                        selected = selectedMessageId == message.id,
                        playing = playback.playingMessageId == message.id,
                        playbackProgress = if (playback.playingMessageId == message.id) playback.progress else .48f,
                        onPlay = { playback.toggle(message, scope) },
                        onLongPress = { selectedMessageId = message.id },
                        onClearSelection = { selectedMessageId = null },
                        onReply = {
                            replyingToId = message.id
                            selectedMessageId = null
                            emojiPanelOpen = false
                            scope.launch {
                                delay(50)
                                focusRequester.requestFocus()
                                keyboard?.show()
                            }
                        },
                        onReaction = { emoji ->
                            dispatch(AppAction.SetMessageReaction(thread.id, message.id, emoji))
                            selectedMessageId = null
                        },
                    )
                }
            }
        }
    }

    if (attachmentSheet) {
        AttachmentSheet(
            onDismiss = { attachmentSheet = false },
            onAction = { action ->
                attachmentSheet = false
                val text = when (action) {
                    "Document" -> "📄 Demo document.pdf"
                    "Catalogue" -> "🛍️ Catalogue item"
                    "Quick Reply" -> "Thanks for contacting Northstar Studio!"
                    "Location" -> "📍 Bengaluru, India"
                    "Contact" -> "👤 Demo Contact"
                    "Poll" -> "📊 Demo poll"
                    "Event" -> "🗓️ Team meetup"
                    else -> "🔳 UPI QR demo"
                }
                dispatch(
                    AppAction.SendText(
                        threadId = thread.id,
                        text = text,
                        replyToSender = replyingTo?.let { if (it.direction == MessageDirection.Outgoing) "You" else thread.contact.name },
                        replyToBody = replyingTo?.body?.ifBlank { "Voice message" },
                    ),
                )
                replyingToId = null
                showMessage("$action attached")
            },
        )
    }
}

@Composable
private fun ChatTopBar(
    thread: ChatThreadUi,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onVideo: () -> Unit,
    onCall: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(AppBackground).statusBarsPadding().height(64.dp).padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
        Row(
            Modifier.weight(1f).clickable(onClick = onInfo).semantics { contentDescription = if (thread.contact.isGroup) "Open group info" else "Open contact info" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(thread.contact, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(thread.contact.name, color = PrimaryText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(thread.contact.subtitle, color = SecondaryText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (thread.contact.isGroup) {
            IconButton(onClick = onCall) { Icon(Icons.Outlined.RecordVoiceOver, "Voice chat", tint = PrimaryText) }
        } else {
            IconButton(onClick = onVideo) { Icon(Icons.Outlined.VideoCall, "Video call", tint = PrimaryText) }
            IconButton(onClick = onCall) { Icon(Icons.Outlined.Call, "Voice call", tint = PrimaryText) }
        }
        IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreVert, "More", tint = PrimaryText) }
    }
}

@Composable
private fun MessageSelectionTopBar(
    onBack: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(ElevatedSurface).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Close selection", tint = PrimaryText) }
        Spacer(Modifier.width(16.dp))
        Text("1", color = PrimaryText, fontSize = 22.sp)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onReply) { Icon(Icons.Rounded.Reply, "Reply", tint = PrimaryText) }
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Delete", tint = PrimaryText) }
        IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, "Copy", tint = PrimaryText) }
        IconButton(onClick = onForward) { Icon(Icons.Rounded.Forward, "Forward", tint = PrimaryText) }
        IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreVert, "More", tint = PrimaryText) }
    }
}

@Composable
private fun MessageBubble(message: MessageUi, voiceContact: com.jp.whatsappclone.data.ContactUi) {
    val outgoing = message.direction == MessageDirection.Outgoing
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start) {
            Box(
                Modifier.then(if (message.voiceSeconds != null) Modifier.width(292.dp) else Modifier.widthIn(max = 310.dp)).clip(
                    RoundedCornerShape(
                        topStart = if (outgoing) 16.dp else 4.dp,
                        topEnd = if (outgoing) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    ),
                ).background(if (outgoing) OutgoingBubble else IncomingBubble).padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                if (message.voiceSeconds != null) {
                    VoiceMessage(message, outgoing, voiceContact)
                } else {
                    Box {
                        Text(
                            text = if (message.deleted) "⊘  You deleted this message" else message.body,
                            color = if (message.deleted) SecondaryText else PrimaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = if (message.deleted) FontStyle.Italic else FontStyle.Normal,
                            modifier = Modifier.padding(end = if (outgoing) 78.dp else 58.dp),
                        )
                        Row(Modifier.align(Alignment.BottomEnd), verticalAlignment = Alignment.CenterVertically) {
                            Text(message.time, color = SecondaryText, fontSize = 11.sp, maxLines = 1)
                            if (outgoing) {
                                Spacer(Modifier.width(3.dp))
                                DeliveryIcon(message.status)
                            }
                        }
                    }
                }
            }
            message.reaction?.let { reaction ->
                Text(
                    reaction,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = (-5).dp).clip(RoundedCornerShape(14.dp)).background(ComponentSurface).padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceMessage(message: MessageUi, outgoing: Boolean, contact: com.jp.whatsappclone.data.ContactUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (outgoing) {
            Avatar(contact, size = 38.dp)
            Spacer(Modifier.width(5.dp))
        }
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.Transparent), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, "Play voice message", tint = SecondaryText, modifier = Modifier.size(32.dp))
        }
        Column(Modifier.width(174.dp)) {
            VoiceWaveform(.48f, Modifier.fillMaxWidth().height(29.dp))
            Row {
                Text(formatSeconds(message.voiceSeconds ?: 0), color = SecondaryText, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(message.time, color = SecondaryText, fontSize = 12.sp)
                if (outgoing) {
                    Spacer(Modifier.width(3.dp))
                    DeliveryIcon(message.status)
                }
            }
        }
        if (!outgoing) {
            Spacer(Modifier.width(5.dp))
            Avatar(contact, size = 38.dp)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MessageBubble(
    message: MessageUi,
    voiceContact: com.jp.whatsappclone.data.ContactUi,
    showSender: Boolean,
    selected: Boolean,
    playing: Boolean,
    playbackProgress: Float,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    onClearSelection: () -> Unit,
    onReply: () -> Unit,
    onReaction: (String) -> Unit,
) {
    val outgoing = message.direction == MessageDirection.Outgoing
    val replyThreshold = with(LocalDensity.current) { 58.dp.toPx() }
    var draggedBy by remember(message.id) { mutableStateOf(0f) }
    val displayedDrag by androidx.compose.animation.core.animateFloatAsState(draggedBy, label = "replyDrag")

    Box(
        Modifier.fillMaxWidth()
            .background(if (selected) Color(0xA88B9093) else Color.Transparent)
            .combinedClickable(
                onClick = { if (selected) onClearSelection() },
                onLongClick = onLongPress,
            )
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        if (amount > 0f || draggedBy > 0f) {
                            change.consume()
                            draggedBy = (draggedBy + amount).coerceIn(0f, replyThreshold * 1.25f)
                        }
                    },
                    onDragEnd = {
                        val shouldReply = draggedBy >= replyThreshold
                        draggedBy = 0f
                        if (shouldReply) onReply()
                    },
                    onDragCancel = { draggedBy = 0f },
                )
            },
    ) {
        if (displayedDrag > 8f) {
            Icon(
                Icons.Rounded.Reply,
                "Reply",
                tint = PrimaryText,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp).size(24.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().offset { androidx.compose.ui.unit.IntOffset(displayedDrag.roundToInt(), 0) },
            horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
        ) {
            Column(horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start) {
                Column(
                    Modifier.then(if (message.voiceSeconds != null) Modifier.width(292.dp) else Modifier.widthIn(max = 310.dp))
                        .clip(
                            RoundedCornerShape(
                                topStart = if (outgoing) 16.dp else 4.dp,
                                topEnd = if (outgoing) 4.dp else 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp,
                            ),
                        )
                        .background(if (outgoing) OutgoingBubble else IncomingBubble)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    if (showSender && message.direction == MessageDirection.Incoming && message.senderName != null) {
                        Text(
                            message.senderName,
                            color = senderColor(message.senderName),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(3.dp))
                    }
                    if (message.replyToBody != null) {
                        ReplyQuote(message.replyToSender ?: "Contact", message.replyToBody)
                        Spacer(Modifier.height(5.dp))
                    }
                    if (message.voiceSeconds != null) {
                        InteractiveVoiceMessage(message, outgoing, voiceContact, playing, playbackProgress, onPlay)
                    } else {
                        Box {
                            Text(
                                text = if (message.deleted) "⊘  You deleted this message" else message.body,
                                color = if (message.deleted) SecondaryText else PrimaryText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = if (message.deleted) FontStyle.Italic else FontStyle.Normal,
                                modifier = Modifier.padding(end = if (outgoing) 78.dp else 58.dp),
                            )
                            Row(Modifier.align(Alignment.BottomEnd), verticalAlignment = Alignment.CenterVertically) {
                                Text(message.time, color = SecondaryText, fontSize = 11.sp, maxLines = 1)
                                if (outgoing) {
                                    Spacer(Modifier.width(3.dp))
                                    DeliveryIcon(message.status)
                                }
                            }
                        }
                    }
                }
                message.reaction?.let { reaction ->
                    Text(
                        reaction,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = (-5).dp)
                            .clip(RoundedCornerShape(14.dp)).background(ComponentSurface)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
        }
        if (selected) {
            ReactionBar(
                selectedReaction = message.reaction,
                onReaction = onReaction,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-56).dp),
            )
        }
    }
}

@Composable
private fun ReplyQuote(sender: String, body: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(Color(0xA60B1418))) {
        Box(Modifier.width(4.dp).height(46.dp).background(Color(0xFF9B76EE)))
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(sender, color = Color(0xFFB497F5), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text(body, color = SecondaryText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ReactionBar(
    selectedReaction: String?,
    onReaction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val trayScale by animateFloatAsState(
        targetValue = if (appeared) 1f else .86f,
        animationSpec = spring(dampingRatio = .72f, stiffness = 480f),
        label = "reactionTrayScale",
    )
    val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🫡")
    Row(
        modifier
            .zIndex(8f)
            .scale(trayScale)
            .shadow(9.dp, RoundedCornerShape(32.dp), clip = false)
            .width(336.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF20282B))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        emojis.forEach { emoji ->
            val active = selectedReaction == emoji
            Box(
                Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(if (active) Color(0xFF3A4448) else Color.Transparent)
                    .clickable { onReaction(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 27.sp)
            }
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF75848C)).clickable { onReaction("😊") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, "More reactions", tint = AppBackground, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun InteractiveVoiceMessage(
    message: MessageUi,
    outgoing: Boolean,
    contact: com.jp.whatsappclone.data.ContactUi,
    playing: Boolean,
    playbackProgress: Float,
    onPlay: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (outgoing) {
            Avatar(contact, size = 38.dp)
            Spacer(Modifier.width(5.dp))
        }
        Box(
            Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (playing) "Pause voice message" else "Play voice message",
                tint = SecondaryText,
                modifier = Modifier.size(32.dp),
            )
        }
        Column(Modifier.width(174.dp)) {
            VoiceWaveform(playbackProgress, Modifier.fillMaxWidth().height(29.dp))
            Row {
                val displayedSeconds = if (playing) ((message.voiceSeconds ?: 0) * playbackProgress).roundToInt() else (message.voiceSeconds ?: 0)
                Text(formatSeconds(displayedSeconds), color = SecondaryText, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(message.time, color = SecondaryText, fontSize = 12.sp)
                if (outgoing) {
                    Spacer(Modifier.width(3.dp))
                    DeliveryIcon(message.status)
                }
            }
        }
        if (!outgoing) {
            Spacer(Modifier.width(5.dp))
            Avatar(contact, size = 38.dp)
        }
    }
}

@Composable
private fun GroupMentionPicker(
    members: List<GroupMemberUi>,
    includeAll: Boolean,
    onSelect: (String) -> Unit,
) {
    val rowCount = members.size + if (includeAll) 1 else 0
    val panelHeight = (rowCount.coerceAtLeast(1) * 66).coerceAtMost(350).dp
    Column(
        Modifier.fillMaxWidth()
            .height(panelHeight)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(ComponentSurface),
    ) {
        if (rowCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No group members found", color = SecondaryText)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (includeAll) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().height(66.dp).clickable { onSelect("all") }.padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF2B3438)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Groups, null, tint = SecondaryText, modifier = Modifier.size(27.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("all", color = PrimaryText, fontSize = 17.sp)
                                Text("Mention all members in this chat", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                items(members, key = { it.contact.id }) { member ->
                    Row(
                        Modifier.fillMaxWidth().height(66.dp).clickable { onSelect(member.contact.name) }.padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(member.contact, size = 42.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(member.contact.name, color = PrimaryText, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (member.about.isNotBlank()) {
                                Text(member.about, color = SecondaryText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    onSend: (String) -> Unit,
    onAttachment: () -> Unit,
    onRecord: () -> Unit,
    onCamera: () -> Unit,
    initialText: String,
) {
    var draft by rememberSaveable { mutableStateOf(initialText) }
    Row(
        Modifier.fillMaxWidth().background(Color.Transparent).imePadding().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(25.dp)).background(ComponentSurface).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) { Icon(Icons.Rounded.EmojiEmotions, "Emoji", tint = SecondaryText) }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText),
                cursorBrush = SolidColor(BrandGreen),
                singleLine = true,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (draft.isBlank()) Text("Message", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    }
                },
            )
            IconButton(onClick = onAttachment) { Icon(Icons.Rounded.AttachFile, "Attach", tint = SecondaryText) }
            IconButton(onClick = onCamera) { Icon(Icons.Outlined.CameraAlt, "Camera", tint = SecondaryText) }
        }
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(ActionWhite).clickable {
                if (draft.isBlank()) onRecord() else {
                    onSend(draft)
                    draft = ""
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (draft.isBlank()) Icons.Rounded.Mic else Icons.Rounded.Send, if (draft.isBlank()) "Record" else "Send", tint = AppBackground, modifier = Modifier.size(27.dp))
        }
    }
}

@Composable
private fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    replyingTo: MessageUi?,
    contactName: String,
    onCancelReply: () -> Unit,
    emojiPanelOpen: Boolean,
    focusRequester: FocusRequester,
    addNavigationPadding: Boolean,
    onFocused: () -> Unit,
    onEmojiToggle: () -> Unit,
    onSend: () -> Unit,
    onAttachment: () -> Unit,
    onRecord: () -> Unit,
    onCamera: () -> Unit,
) {
    val baseModifier = if (addNavigationPadding) Modifier.navigationBarsPadding() else Modifier
    Column(baseModifier.fillMaxWidth().background(Color.Transparent)) {
        if (replyingTo != null) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xE80B1014)).padding(start = 16.dp, end = 8.dp, top = 7.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(4.dp).height(48.dp).background(Color(0xFF9B76EE)))
                Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                    Text(
                        if (replyingTo.direction == MessageDirection.Outgoing) "You" else contactName,
                        color = Color(0xFFB497F5),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        replyingTo.body.ifBlank { "Voice message" },
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onCancelReply) { Icon(Icons.Rounded.Delete, "Cancel reply", tint = SecondaryText) }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(25.dp)).background(ComponentSurface).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onEmojiToggle) {
                    Icon(if (emojiPanelOpen) Icons.Rounded.Keyboard else Icons.Rounded.EmojiEmotions, if (emojiPanelOpen) "Show keyboard" else "Emoji", tint = SecondaryText)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged { if (it.isFocused) onFocused() },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText),
                    cursorBrush = SolidColor(BrandGreen),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (value.isNotBlank()) onSend() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isBlank()) Text("Message", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
                            inner()
                        }
                    },
                )
                IconButton(onClick = onAttachment) { Icon(Icons.Rounded.AttachFile, "Attach", tint = SecondaryText) }
                IconButton(onClick = onCamera) { Icon(Icons.Outlined.CameraAlt, "Camera", tint = SecondaryText) }
            }
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(ActionWhite).clickable {
                    if (value.isBlank()) onRecord() else onSend()
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (value.isBlank()) Icons.Rounded.Mic else Icons.Rounded.Send, if (value.isBlank()) "Record" else "Send", tint = AppBackground, modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun EmojiPicker(onEmoji: (String) -> Unit, onBackspace: () -> Unit) {
    val emojis = listOf(
        "😊", "😂", "🌚", "❤️", "👀", "😍", "😴", "🤝",
        "😁", "🙈", "✅", "❌", "✨", "🙂", "🥳", "🫡",
        "👍", "🤔", "😐", "💙", "🤗", "🙄", "🤷", "🔥",
        "😵", "😭", "😹", "😮", "😬", "🥲", "😌", "🤦",
    )
    Column(
        Modifier.fillMaxWidth().height(286.dp).background(ElevatedSurface).navigationBarsPadding().padding(horizontal = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, "Search emoji", tint = PrimaryText, modifier = Modifier.padding(horizontal = 12.dp))
            Row(
                Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(18.dp)).background(ComponentSurface),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.EmojiEmotions, "Emoji", tint = PrimaryText)
                Text("GIF", color = SecondaryText, fontWeight = FontWeight.Bold)
                Text("◯", color = SecondaryText, fontSize = 22.sp)
            }
            IconButton(onClick = onBackspace) { Icon(Icons.Rounded.Backspace, "Backspace", tint = PrimaryText) }
        }
        Text("Recents", color = PrimaryText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.weight(1f),
            userScrollEnabled = true,
        ) {
            gridItems(emojis) { emoji ->
                Box(Modifier.height(46.dp).clickable { onEmoji(emoji) }, contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 27.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(38.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("◷", "☺", "❀", "☕", "⚽", "🚗", "💡", "⚑").forEach { category ->
                Text(category, color = SecondaryText, fontSize = 21.sp)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AttachmentSheet(onDismiss: () -> Unit, onAction: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ElevatedSurface,
        contentColor = PrimaryText,
        dragHandle = {
            Box(Modifier.padding(vertical = 10.dp).width(32.dp).height(4.dp).clip(RoundedCornerShape(3.dp)).background(SecondaryText))
        },
    ) {
        val actions = listOf(
            Triple("Document", Icons.Rounded.Description, Color(0xFF8B62EB)),
            Triple("Catalogue", Icons.Rounded.Storefront, Color(0xFF46636B)),
            Triple("Quick Reply", Icons.Rounded.Bolt, Color(0xFFFFB928)),
            Triple("Location", Icons.Rounded.LocationOn, AttachmentGreen),
            Triple("Contact", Icons.Rounded.ContactPage, Color(0xFF1F9CDA)),
            Triple("Poll", Icons.Rounded.Poll, Color(0xFFFFB33B)),
            Triple("Event", Icons.Rounded.Event, Color(0xFFEE3C91)),
            Triple("Share UPI QR", Icons.Rounded.QrCode2, Color(0xFF1578E7)),
        )
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            actions.chunked(4).forEach { rowActions ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    rowActions.forEach { (label, icon, color) -> SheetAction(label, icon, color) { onAction(label) } }
                }
            }
            LazyRow(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                items(listOf(0xFF24443EL, 0xFF433046L, 0xFF3B4A6CL, 0xFF6B482EL, 0xFF275D57L)) { color ->
                    RecentMediaThumbnail(Color(color)) { onAction("Document") }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(Modifier.width(78.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(60.dp).height(32.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0xFF30383D), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = color, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = SecondaryText, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun RecentMediaThumbnail(accent: Color, onClick: () -> Unit) {
    Canvas(
        Modifier.width(92.dp).height(128.dp).clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1518)).clickable(onClick = onClick),
    ) {
        drawRect(Color(0xFF10191D))
        drawRect(Color(0xFF172126), size = Size(size.width, size.height * .12f))
        drawCircle(accent, size.width * .045f, Offset(size.width * .1f, size.height * .06f))
        for (index in 0 until 7) {
            val outgoing = index % 3 != 0
            val bubbleWidth = size.width * (.48f + ((index * 17) % 25) / 100f)
            val bubbleHeight = size.height * if (index == 2 || index == 5) .105f else .072f
            val left = if (outgoing) size.width - bubbleWidth - size.width * .055f else size.width * .055f
            val top = size.height * (.17f + index * .115f)
            drawRoundRect(
                color = if (outgoing) Color(0xFF134D37) else Color(0xFF1F272A),
                topLeft = Offset(left, top),
                size = Size(bubbleWidth, bubbleHeight),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            drawRoundRect(
                color = Color(0x668696A0),
                topLeft = Offset(left + 4.dp.toPx(), top + 3.dp.toPx()),
                size = Size(bubbleWidth * .58f, 1.2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun RecordingPanel(
    seconds: Int,
    paused: Boolean,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onSend: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(ElevatedSurface).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 18.dp),
    ) {
        Row(Modifier.padding(start = 10.dp, end = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatSeconds(seconds), color = PrimaryText, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            VoiceWaveform((seconds % 10) / 10f, Modifier.weight(1f).height(35.dp), playedColor = ActionWhite)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.Mic, "Recording", tint = ActionWhite)
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF431724)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Delete, "Delete recording", tint = DestructiveRed)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF2B3034)).clickable(onClick = onPause),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = PrimaryText)
                Spacer(Modifier.width(10.dp))
                Text(if (paused) "Resume" else "Pause", color = PrimaryText, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(52.dp).clip(CircleShape).background(ActionWhite).clickable(onClick = onSend), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Send, "Send recording", tint = AppBackground)
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun senderColor(name: String): Color {
    val palette = listOf(
        Color(0xFFE579A4),
        Color(0xFF53BDEB),
        Color(0xFFFFB74D),
        Color(0xFF9B76EE),
        Color(0xFF25D366),
    )
    return palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
}

private fun String.activeMentionQuery(): String? {
    val marker = lastIndexOf('@')
    if (marker < 0 || (marker > 0 && !this[marker - 1].isWhitespace())) return null
    val query = substring(marker + 1)
    return query.takeIf { value -> value.none { it.isWhitespace() } }
}

private fun String.replaceActiveMention(name: String): String {
    val marker = lastIndexOf('@')
    if (marker < 0) return this
    return substring(0, marker) + "@$name "
}

private fun String.removeLastCodePoint(): String {
    if (isEmpty()) return this
    val lastIndex = lastIndex
    val removeFrom = if (
        lastIndex > 0 &&
        Character.isLowSurrogate(this[lastIndex]) &&
        Character.isHighSurrogate(this[lastIndex - 1])
    ) {
        lastIndex - 1
    } else {
        lastIndex
    }
    return substring(0, removeFrom)
}
