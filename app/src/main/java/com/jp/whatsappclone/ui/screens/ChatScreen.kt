package com.jp.whatsappclone.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Forward
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.jp.whatsappclone.data.AppAction
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.data.ChatThreadUi
import com.jp.whatsappclone.data.DeliveryStatus
import com.jp.whatsappclone.data.GroupMemberUi
import com.jp.whatsappclone.data.MessageDirection
import com.jp.whatsappclone.data.MessageUi
import com.jp.whatsappclone.data.ReactionSummaryUi
import com.jp.whatsappclone.data.domain.MessageKind
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
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ChatScreen(
    threadId: String,
    state: AppState,
    dispatch: (AppAction) -> Unit,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
    pagedMessages: LazyPagingItems<MessageUi>? = null,
    previewMessages: List<MessageUi> = emptyList(),
    onInfo: () -> Unit = {},
    initialAttachmentSheet: Boolean = false,
    initialComposerText: String = "",
    persistedDraft: String? = null,
    onDraftChanged: (String) -> Unit = {},
    typingMemberNames: Set<String> = emptySet(),
    onLoadOlder: () -> Unit = {},
    hasMoreHistory: Boolean = false,
    historyLoading: Boolean = false,
    onNewestMessageVisible: (String) -> Unit = {},
    onForwardMessage: (String) -> Unit = {},
    onPrepareAttachment: suspend (String) -> Result<String> = { Result.failure(IllegalStateException("Attachment unavailable")) },
) {
    val thread = state.chats.firstOrNull { it.id == threadId } ?: return
    // Production is newest-first PagingData rendered with reverseLayout. Debug previews may pass
    // a bounded oldest-first fixture explicitly without embedding messages in ChatThreadUi.
    val fallbackMessages = remember(previewMessages) { previewMessages.asReversed() }
    val messageCount = pagedMessages?.itemCount ?: fallbackMessages.size
    val messageKey = pagedMessages?.itemKey(MessageUi::id) ?: { index: Int ->
        fallbackMessages[index].id
    }
    fun peekMessage(index: Int): MessageUi? = when {
        index !in 0 until messageCount -> null
        pagedMessages != null -> pagedMessages.peek(index)
        else -> fallbackMessages.getOrNull(index)
    }
    var attachmentSheet by rememberSaveable { mutableStateOf(initialAttachmentSheet) }
    var draft by rememberSaveable(threadId) { mutableStateOf(initialComposerText) }
    var emojiPanelOpen by rememberSaveable { mutableStateOf(false) }
    var replyingToId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var previousLastMessageId by remember(threadId) { mutableStateOf<String?>(null) }
    var recordingActive by rememberSaveable(threadId) { mutableStateOf(false) }
    var recordingPaused by rememberSaveable(threadId) { mutableStateOf(false) }
    var recordingSeconds by rememberSaveable(threadId) { mutableStateOf(0) }
    var mockAttachmentAction by rememberSaveable(threadId) { mutableStateOf<String?>(null) }
    var photoViewerMessageId by rememberSaveable(threadId) { mutableStateOf<String?>(null) }
    var photoViewerPath by rememberSaveable(threadId) { mutableStateOf<String?>(null) }
    var mediaComposerUris by rememberSaveable(threadId) { mutableStateOf<List<String>>(emptyList()) }
    var mediaComposerIndex by rememberSaveable(threadId) { mutableStateOf(0) }
    var pendingCameraUri by rememberSaveable(threadId) { mutableStateOf<String?>(null) }
    val preparedAttachmentPaths = remember(threadId) { mutableStateMapOf<String, String>() }
    val requestedPdfPreviews = remember(threadId) { mutableStateMapOf<String, Boolean>() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var galleryAccessGranted by remember {
        mutableStateOf(context.hasGalleryImageAccess())
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        galleryAccessGranted = result.values.any { it } || context.hasGalleryImageAccess()
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val recorder = remember(context) { VoiceRecorder(context) }
    val playback = remember(context) { VoicePlaybackController(context) }
    val clipboard = LocalClipboardManager.current
    val loadedMessages = pagedMessages?.itemSnapshotList?.items ?: fallbackMessages
    val replyingTo = loadedMessages.firstOrNull { it.id == replyingToId }
    val selectedMessage = loadedMessages.firstOrNull { it.id == selectedMessageId }
    val groupMembers = state.groupMembers.filter { it.groupId == thread.id }
    val mentionQuery = if (thread.contact.isGroup && !emojiPanelOpen) draft.activeMentionQuery() else null
    val mentionMembers = if (mentionQuery != null) {
        groupMembers
            .filter { it.contact.name != "You" && it.contact.name.contains(mentionQuery, ignoreCase = true) }
            .sortedBy { member -> member.contact.name.lowercase() }
            .take(6)
    } else {
        emptyList()
    }

    val startRecording = {
        if (recorder.start()) {
            recordingActive = true
            recordingPaused = false
            recordingSeconds = 0
            emojiPanelOpen = false
            focusManager.clearFocus()
            keyboard?.hide()
        } else showMessage("Unable to start microphone")
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else showMessage("Microphone permission is required for voice messages")
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val metadata = context.resolveAttachmentMetadata(it)
            dispatch(
                AppAction.SendAttachment(
                    threadId = thread.id,
                    sourceUri = it.toString(),
                    kind = MessageKind.DOCUMENT,
                    fileName = metadata.first,
                    mimeType = context.contentResolver.getType(it).orEmpty(),
                    sizeBytes = metadata.second,
                    replyToMessageId = replyingTo?.id,
                ),
            )
            replyingToId = null
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            mediaComposerUris = (mediaComposerUris + uris.map(Uri::toString)).distinct().take(30)
            mediaComposerIndex = mediaComposerIndex.coerceIn(mediaComposerUris.indices)
        }
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val capturedUri = pendingCameraUri
        if (captured && capturedUri != null) {
            mediaComposerUris = listOf(capturedUri)
            mediaComposerIndex = 0
        } else {
            capturedUri?.let { context.deleteCameraCapture(Uri.parse(it)) }
        }
        pendingCameraUri = null
    }
    val launchCamera = {
        val output = context.createCameraCaptureUri()
        pendingCameraUri = output.toString()
        cameraCapture.launch(output)
    }

    LaunchedEffect(recordingActive, recordingPaused) {
        while (recordingActive) {
            delay(1_000)
            if (recordingActive && !recordingPaused) recordingSeconds++
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            playback.release()
        }
    }

    LaunchedEffect(persistedDraft) {
        if (draft.isEmpty() && !persistedDraft.isNullOrEmpty()) draft = persistedDraft
    }
    LaunchedEffect(draft) { onDraftChanged(draft) }

    val newestMessageId = peekMessage(0)?.id
    LaunchedEffect(newestMessageId) {
        val latestId = newestMessageId ?: return@LaunchedEffect
        val firstPosition = previousLastMessageId == null
        val wasNearBottom = listState.firstVisibleItemIndex <= 1
        previousLastMessageId = latestId
        if (firstPosition || wasNearBottom) listState.animateScrollToItem(0)
        onNewestMessageVisible(latestId)
    }
    LaunchedEffect(
        listState,
        messageCount,
        hasMoreHistory,
        historyLoading,
    ) {
        if (!hasMoreHistory || historyLoading || messageCount == 0) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
        }
            .distinctUntilChanged()
            .collect { oldestVisibleIndex ->
                if (oldestVisibleIndex >= messageCount - 3) onLoadOlder()
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
                        if (selectedMessage.direction == MessageDirection.Outgoing && !selectedMessage.deleted) {
                            dispatch(AppAction.DeleteMessage(thread.id, selectedMessage.id))
                        } else {
                            showMessage(V1_UNAVAILABLE_MESSAGE)
                        }
                        selectedMessageId = null
                    },
                    onCopy = {
                        val copyText = when {
                            selectedMessage.deleted -> "This message was deleted"
                            selectedMessage.voiceSeconds != null -> "Voice message"
                            else -> selectedMessage.body
                        }
                        clipboard.setText(AnnotatedString(copyText))
                        selectedMessageId = null
                        showMessage("Message copied")
                    },
                    onForward = {
                        onForwardMessage(selectedMessage.id)
                        selectedMessageId = null
                    },
                    onMore = {
                        if (selectedMessage.status == DeliveryStatus.Failed) {
                            dispatch(AppAction.RetryMessage(thread.id, selectedMessage.id))
                            selectedMessageId = null
                        } else {
                            showMessage(V1_UNAVAILABLE_MESSAGE)
                        }
                    },
                )
            } else {
                ChatTopBar(
                    thread = thread,
                    typingMemberNames = typingMemberNames,
                    onBack = onBack,
                    onInfo = onInfo,
                    onVideo = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                    onCall = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                    onMore = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                )
            }
        },
        bottomBar = {
            Column(Modifier.imePadding()) {
                if (recordingActive) {
                    RecordingPanel(
                        seconds = recordingSeconds,
                        paused = recordingPaused,
                        onDelete = {
                            recorder.cancel()
                            recordingActive = false
                            recordingPaused = false
                            recordingSeconds = 0
                        },
                        onPause = {
                            val changed = if (recordingPaused) recorder.resume() else recorder.pause()
                            if (changed) recordingPaused = !recordingPaused
                        },
                        onSend = {
                            val file = recorder.stop(keepFile = true)
                            if (file != null) {
                                dispatch(
                                    AppAction.SendAttachment(
                                        threadId = thread.id,
                                        sourceUri = Uri.fromFile(file).toString(),
                                        kind = MessageKind.AUDIO,
                                        fileName = file.name,
                                        mimeType = "audio/mp4",
                                        sizeBytes = file.length(),
                                        durationMillis = recordingSeconds.coerceAtLeast(1) * 1_000L,
                                        replyToMessageId = replyingTo?.id,
                                    ),
                                )
                                replyingToId = null
                            } else showMessage("Voice recording was too short")
                            recordingActive = false
                            recordingPaused = false
                            recordingSeconds = 0
                        },
                    )
                } else {
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
                        onValueChange = { draft = it.take(MAX_TEXT_LENGTH) },
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
                                        replyToMessageId = replyingTo?.id,
                                        replyToSender = replyingTo?.let { if (it.direction == MessageDirection.Outgoing) "You" else thread.contact.name },
                                        replyToBody = replyingTo?.body?.ifBlank { "Voice message" },
                                    ),
                                )
                                draft = ""
                                replyingToId = null
                            }
                        },
                        onAttachment = {
                            attachmentSheet = true
                            if (!galleryAccessGranted) {
                                galleryPermissionLauncher.launch(galleryImagePermissions())
                            }
                        },
                        onRecord = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startRecording()
                            } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onCamera = launchCamera,
                    )
                    if (emojiPanelOpen) {
                        EmojiPicker(
                            onEmoji = { emoji -> draft = (draft + emoji).take(MAX_TEXT_LENGTH) },
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
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                reverseLayout = true,
            ) {
                items(messageCount, key = messageKey) { index ->
                    val message = if (pagedMessages != null) {
                        pagedMessages[index]
                    } else {
                        fallbackMessages.getOrNull(index)
                    } ?: return@items
                    val voiceContact = if (message.direction == MessageDirection.Outgoing) {
                        state.contacts.firstOrNull { it.id == "own" } ?: thread.contact
                    } else {
                        thread.contact
                    }
                    val preparedPath = message.mediaPath ?: preparedAttachmentPaths[message.id]
                    val isPdfDocument = message.kind == MessageKind.DOCUMENT &&
                        (message.mimeType.equals("application/pdf", ignoreCase = true) ||
                            message.fileName?.endsWith(".pdf", ignoreCase = true) == true)
                    LaunchedEffect(message.id, preparedPath, isPdfDocument) {
                        if (isPdfDocument && preparedPath == null && requestedPdfPreviews[message.id] != true) {
                            requestedPdfPreviews[message.id] = true
                            onPrepareAttachment(message.id).onSuccess { localPath ->
                                preparedAttachmentPaths[message.id] = localPath
                            }
                        }
                    }
                    val displayMessage = if (preparedPath != null && preparedPath != message.mediaPath) {
                        message.copy(mediaPath = preparedPath)
                    } else {
                        message
                    }
                    Column {
                        val older = peekMessage(index + 1)
                        if (older == null || !sameLocalDay(older.clientCreatedAtMillis, message.clientCreatedAtMillis)) {
                            Box(
                                Modifier.fillMaxWidth().padding(bottom = 5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    dateHeaderLabel(message.clientCreatedAtMillis),
                                    color = SecondaryText,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(ElevatedSurface)
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                )
                            }
                        }
                        MessageBubble(
                            message = displayMessage,
                            voiceContact = voiceContact,
                            showSender = thread.contact.isGroup,
                            selected = selectedMessageId == message.id,
                            playbackActive = playback.activeMessageId == message.id,
                            playing = playback.activeMessageId == message.id && playback.isPlaying,
                            playbackProgress = if (playback.activeMessageId == message.id) playback.progress else restingVoiceProgress(message.id),
                            playbackSpeed = if (playback.activeMessageId == message.id) playback.speed else 1f,
                            onPlay = {
                                scope.launch {
                                    val source = message.audioPath ?: onPrepareAttachment(message.id)
                                        .onFailure { showMessage(it.message ?: "Unable to download voice message") }
                                        .getOrNull()
                                    if (source != null) playback.toggle(message, source, scope)
                                }
                            },
                            onSpeed = { if (playback.activeMessageId == message.id) playback.cycleSpeed() },
                            onOpenAttachment = {
                                scope.launch {
                                    onPrepareAttachment(message.id)
                                        .onSuccess { localUri ->
                                            if (message.kind == MessageKind.IMAGE) {
                                                photoViewerMessageId = message.id
                                                photoViewerPath = localUri
                                            } else {
                                                runCatching {
                                                    context.openAttachmentChooser(
                                                        localUri = localUri,
                                                        mimeType = message.mimeType,
                                                        displayName = message.fileName,
                                                    )
                                                }.onFailure {
                                                    showMessage(it.message ?: "No app can open this attachment")
                                                }
                                            }
                                        }
                                        .onFailure { showMessage(it.message ?: "Unable to download attachment") }
                                }
                            },
                            onLongPress = {
                                focusManager.clearFocus(force = true)
                                keyboard?.hide()
                                emojiPanelOpen = false
                                selectedMessageId = message.id
                            },
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
                                if (message.deleted) {
                                    showMessage(V1_UNAVAILABLE_MESSAGE)
                                } else {
                                    val nextReaction = emoji.takeUnless { it == message.ownReaction }
                                    dispatch(AppAction.SetMessageReaction(thread.id, message.id, nextReaction))
                                }
                                selectedMessageId = null
                            },
                            onMoreReaction = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                        )
                    }
                }
                if (historyLoading) {
                    item(key = "history-loading") {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = BrandGreen,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
    }

    if (attachmentSheet) {
        AttachmentSheet(
            galleryAccessGranted = galleryAccessGranted,
            onDismiss = { attachmentSheet = false },
            onRequestGalleryAccess = {
                galleryPermissionLauncher.launch(galleryImagePermissions())
            },
            onMediaSelected = { selectedMedia ->
                attachmentSheet = false
                mediaComposerUris = selectedMedia.map { it.uri.toString() }.distinct().take(30)
                mediaComposerIndex = 0
            },
            onBrowsePhotos = {
                attachmentSheet = false
                imagePicker.launch("image/*")
            },
            onAction = { action ->
                attachmentSheet = false
                when (action) {
                    "Document" -> documentPicker.launch(arrayOf("application/pdf", "text/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/octet-stream"))
                    "Photo" -> imagePicker.launch("image/*")
                    else -> mockAttachmentAction = action
                }
            },
        )
    }
    if (mediaComposerUris.isNotEmpty()) {
        MediaComposer(
            uris = mediaComposerUris,
            selectedIndex = mediaComposerIndex,
            recipient = thread.contact.name,
            onSelect = { mediaComposerIndex = it },
            onAddMore = { imagePicker.launch("image/*") },
            onRemove = { index ->
                val removed = mediaComposerUris.getOrNull(index)
                mediaComposerUris = mediaComposerUris.filterIndexed { itemIndex, _ -> itemIndex != index }
                mediaComposerIndex = mediaComposerIndex.coerceAtMost((mediaComposerUris.size - 1).coerceAtLeast(0))
                if (removed == pendingCameraUri) pendingCameraUri = null
            },
            onDismiss = { mediaComposerUris = emptyList() },
            onSend = { caption, captionIndex ->
                mediaComposerUris.forEachIndexed { index, source ->
                    val uri = Uri.parse(source)
                    val metadata = context.resolveAttachmentMetadata(uri)
                    dispatch(
                        AppAction.SendAttachment(
                            threadId = thread.id,
                            sourceUri = source,
                            kind = MessageKind.IMAGE,
                            fileName = metadata.first,
                            mimeType = context.contentResolver.getType(uri) ?: "image/jpeg",
                            sizeBytes = metadata.second,
                            caption = caption.takeIf { index == captionIndex && it.isNotBlank() },
                            replyToMessageId = replyingTo?.id,
                        ),
                    )
                }
                replyingToId = null
                mediaComposerUris = emptyList()
            },
        )
    }
    val photoViewerMessage = loadedMessages.firstOrNull { it.id == photoViewerMessageId }
    if (photoViewerMessage != null && photoViewerPath != null) {
        PhotoViewer(
            message = photoViewerMessage,
            localUri = photoViewerPath!!,
            ownName = "You",
            contactName = thread.contact.name,
            onDismiss = {
                photoViewerMessageId = null
                photoViewerPath = null
            },
            onReply = {
                replyingToId = photoViewerMessage.id
                photoViewerMessageId = null
                photoViewerPath = null
                scope.launch {
                    delay(50)
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
            },
            onReaction = { emoji ->
                dispatch(AppAction.SetMessageReaction(thread.id, photoViewerMessage.id, emoji))
                showMessage("Reacted $emoji")
            },
            onMoreReaction = { showMessage(V1_UNAVAILABLE_MESSAGE) },
            onShare = {
                runCatching {
                    context.shareLocalAttachment(
                        localUri = photoViewerPath!!,
                        mimeType = photoViewerMessage.mimeType,
                        displayName = photoViewerMessage.fileName,
                    )
                }.onFailure { showMessage(it.message ?: "Unable to share photo") }
            },
        )
    }
    mockAttachmentAction?.let { action ->
        MockAttachmentDialog(
            action = action,
            onDismiss = { mockAttachmentAction = null },
            onSend = {
                dispatch(
                    AppAction.SendText(
                        threadId = thread.id,
                        text = mockAttachmentMessage(action),
                        replyToMessageId = replyingTo?.id,
                    ),
                )
                replyingToId = null
                mockAttachmentAction = null
            },
        )
    }
}

private const val V1_UNAVAILABLE_MESSAGE = "Not available in this version"
private const val MAX_TEXT_LENGTH = 4_000

private fun mockAttachmentMessage(action: String): String = when (action) {
    "Catalogue" -> "🛍️ Catalogue shared: INNOVFIX Services"
    "Quick Reply" -> "Thanks for contacting INNOVFIX. We’ll get back to you shortly."
    "Location" -> "📍 INNOVFIX Office · Bengaluru"
    "Contact" -> "👤 Ayush Co founder · INNOVFIX"
    "Poll" -> "📊 Poll: When should we schedule the team meeting?"
    "Event" -> "📅 Event: INNOVFIX team meeting · Tomorrow, 10:00 am"
    "Share UPI QR" -> "🔳 UPI QR shared · Demo only — no payment initiated"
    else -> action
}

@Composable
private fun MockAttachmentDialog(action: String, onDismiss: () -> Unit, onSend: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedSurface,
        title = { Text(action, color = PrimaryText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(mockAttachmentMessage(action), color = PrimaryText)
                Spacer(Modifier.height(8.dp))
                Text("Mock business content — no external service or payment is used.", color = SecondaryText, fontSize = 13.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryText) } },
        confirmButton = { TextButton(onClick = onSend) { Text("Send", color = BrandGreen, fontWeight = FontWeight.Bold) } },
    )
}

@Composable
private fun ChatTopBar(
    thread: ChatThreadUi,
    typingMemberNames: Set<String>,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onVideo: () -> Unit,
    onCall: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(AppBackground).statusBarsPadding().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
        Spacer(Modifier.width(4.dp))
        Row(
            Modifier.weight(1f).clickable(onClick = onInfo).semantics { contentDescription = if (thread.contact.isGroup) "Open group info" else "Open contact info" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(thread.contact, size = 36.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(thread.contact.name, color = PrimaryText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    typingMemberNames.takeIf { it.isNotEmpty() }?.joinToString { "$it is typing…" }
                        ?: thread.contact.subtitle,
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
        Modifier.fillMaxWidth().background(Color(0xFF12181C)).statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
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
@OptIn(ExperimentalFoundationApi::class)
private fun MessageBubble(
    message: MessageUi,
    voiceContact: com.jp.whatsappclone.data.ContactUi,
    showSender: Boolean,
    selected: Boolean,
    playbackActive: Boolean,
    playing: Boolean,
    playbackProgress: Float,
    playbackSpeed: Float,
    onPlay: () -> Unit,
    onSpeed: () -> Unit,
    onOpenAttachment: () -> Unit,
    onLongPress: () -> Unit,
    onClearSelection: () -> Unit,
    onReply: () -> Unit,
    onReaction: (String) -> Unit,
    onMoreReaction: () -> Unit,
) {
    val outgoing = message.direction == MessageDirection.Outgoing
    val replyThreshold = with(LocalDensity.current) { 58.dp.toPx() }
    var draggedBy by remember(message.id) { mutableStateOf(0f) }
    val displayedDrag by androidx.compose.animation.core.animateFloatAsState(draggedBy, label = "replyDrag")

    Box(
        Modifier.fillMaxWidth()
            .background(if (selected) Color(0x8F8B9093) else Color.Transparent)
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
            Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                .offset { androidx.compose.ui.unit.IntOffset(displayedDrag.roundToInt(), 0) },
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
                        InteractiveVoiceMessage(
                            message = message,
                            outgoing = outgoing,
                            contact = voiceContact,
                            active = playbackActive,
                            playing = playing,
                            playbackProgress = playbackProgress,
                            playbackSpeed = playbackSpeed,
                            onPlay = onPlay,
                            onSpeed = onSpeed,
                        )
                    } else if (!message.deleted && message.kind != MessageKind.TEXT) {
                        AttachmentMessageContent(message, onOpenAttachment)
                        if (message.kind == MessageKind.IMAGE && message.body.isNotBlank() && message.body != "Photo") {
                            Text(
                                message.body,
                                color = PrimaryText,
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
                            )
                        }
                        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                            Text(message.time, color = SecondaryText, fontSize = 11.sp)
                            if (outgoing) {
                                Spacer(Modifier.width(3.dp))
                                DeliveryIcon(message.status)
                            }
                        }
                    } else {
                        Box {
                            Text(
                                text = if (message.deleted) {
                                    if (outgoing) "⊘  You deleted this message" else "⊘  This message was deleted"
                                } else {
                                    message.body
                                },
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
                ReactionSummary(
                    summaries = message.reactionSummaries,
                    ownReaction = message.ownReaction,
                )
            }
        }
        if (selected) {
            ReactionBar(
                selectedReaction = message.ownReaction,
                onReaction = onReaction,
                onMore = onMoreReaction,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-49).dp),
            )
        }
    }
}

@Composable
private fun AttachmentMessageContent(message: MessageUi, onOpen: () -> Unit) {
    val context = LocalContext.current
    when (message.kind) {
        MessageKind.IMAGE -> {
            val bitmap = remember(message.mediaPath) {
                message.mediaPath?.let(Uri::parse)?.let { uri ->
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                    }.getOrNull()
                }
            }
            Box(
                Modifier.width(270.dp).height(170.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF182126)).clickable(onClick = onOpen),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = message.fileName ?: "Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                ) else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = SecondaryText, modifier = Modifier.size(38.dp))
                        Spacer(Modifier.height(7.dp))
                        Text("Tap to download photo", color = SecondaryText, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        MessageKind.DOCUMENT -> {
            val isPdf = message.mimeType.equals("application/pdf", ignoreCase = true) ||
                message.fileName?.endsWith(".pdf", ignoreCase = true) == true
            val pdfPreview by produceState<PdfPreview?>(
                initialValue = null,
                key1 = message.mediaPath,
                key2 = isPdf,
            ) {
                value = if (isPdf) withContext(Dispatchers.IO) {
                    renderPdfPreview(context, message.mediaPath)
                } else null
            }
            Column(
                Modifier.width(270.dp).clickable(onClick = onOpen),
            ) {
                if (pdfPreview != null) {
                    Image(
                        bitmap = pdfPreview!!.bitmap.asImageBitmap(),
                        contentDescription = "${message.fileName ?: "PDF"} first page",
                        modifier = Modifier.fillMaxWidth().height(126.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                    )
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0x8F0B1418)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Description, null, tint = PrimaryText, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    message.fileName ?: "Document",
                    color = PrimaryText,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    documentMetadata(message, pdfPreview?.pageCount),
                    color = SecondaryText,
                    fontSize = 12.sp,
                )
            }
        }
        else -> Unit
    }
}

private data class PdfPreview(val bitmap: Bitmap, val pageCount: Int)

private fun renderPdfPreview(context: Context, localPath: String?): PdfPreview? = runCatching {
    val uri = localPath?.let(Uri::parse) ?: return@runCatching null
    val descriptor = when (uri.scheme) {
        "file" -> ParcelFileDescriptor.open(File(requireNotNull(uri.path)), ParcelFileDescriptor.MODE_READ_ONLY)
        "content" -> context.contentResolver.openFileDescriptor(uri, "r")
        else -> null
    } ?: return@runCatching null
    descriptor.use { parcel ->
        PdfRenderer(parcel).use { renderer ->
            if (renderer.pageCount == 0) return@use null
            renderer.openPage(0).use { page ->
                val targetWidth = 720
                val targetHeight = (page.height * (targetWidth / page.width.toFloat())).roundToInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(AndroidColor.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                PdfPreview(bitmap, renderer.pageCount)
            }
        }
    }
}.getOrNull()

private fun documentMetadata(message: MessageUi, pageCount: Int?): String {
    val pages = pageCount?.let { "$it ${if (it == 1) "page" else "pages"} • " }.orEmpty()
    val extension = message.fileName?.substringAfterLast('.', "")?.uppercase(Locale.US)
        ?.takeIf(String::isNotBlank) ?: "FILE"
    return "$pages${formatDocumentFileSize(message.sizeBytes)} • $extension"
}

private fun formatDocumentFileSize(bytes: Long?): String = when {
    bytes == null -> "Document"
    bytes >= 1_000_000 -> "%.1f MB".format(Locale.US, bytes / 1_000_000f)
    bytes >= 1_000 -> "%.0f KB".format(Locale.US, bytes / 1_000f)
    else -> "$bytes B"
}

private fun Context.openAttachmentChooser(localUri: String, mimeType: String?, displayName: String?) {
    val shareableUri = attachmentContentUri(localUri)
    val resolvedType = mimeType?.takeIf(String::isNotBlank)
        ?: contentResolver.getType(shareableUri)
        ?: "application/octet-stream"
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(shareableUri, resolvedType)
        clipData = ClipData.newRawUri(displayName ?: "Attachment", shareableUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(viewIntent, "Open with"))
}

private fun Context.shareLocalAttachment(localUri: String, mimeType: String?, displayName: String?) {
    val shareableUri = attachmentContentUri(localUri)
    val resolvedType = mimeType?.takeIf(String::isNotBlank)
        ?: contentResolver.getType(shareableUri)
        ?: "image/*"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = resolvedType
        putExtra(Intent.EXTRA_STREAM, shareableUri)
        clipData = ClipData.newRawUri(displayName ?: "Photo", shareableUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, "Share photo"))
}

private fun Context.attachmentContentUri(localUri: String): Uri {
    val source = Uri.parse(localUri)
    return when (source.scheme) {
        "file" -> FileProvider.getUriForFile(this, "$packageName.files", File(requireNotNull(source.path)))
        "content" -> source
        else -> error("Attachment location is unsupported")
    }
}

@Composable
private fun PhotoViewer(
    message: MessageUi,
    localUri: String,
    ownName: String,
    contactName: String,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onReaction: (String) -> Unit,
    onMoreReaction: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    var starred by rememberSaveable(message.id) { mutableStateOf(false) }
    var controlsVisible by rememberSaveable(message.id) { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = localUri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(localUri)
                when (uri.scheme) {
                    "file" -> BitmapFactory.decodeFile(requireNotNull(uri.path))
                    "content" -> context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                    else -> null
                }
            }.getOrNull()
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black).clickable { controlsVisible = !controlsVisible },
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = message.fileName ?: "Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            } else {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = BrandGreen)
            }
            if (controlsVisible) {
                Row(
                    Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        .background(Color(0xE60B1014)).statusBarsPadding().height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (message.direction == MessageDirection.Outgoing) ownName else contactName,
                            color = PrimaryText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            viewerDateLabel(message.clientCreatedAtMillis),
                            color = PrimaryText,
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(onClick = { starred = !starred }) {
                        Icon(
                            if (starred) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                            "Star photo",
                            tint = if (starred) Color(0xFFFFC107) else PrimaryText,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Rounded.Forward, "Share photo", tint = PrimaryText, modifier = Modifier.size(28.dp))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, "More", tint = PrimaryText, modifier = Modifier.size(28.dp))
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = ElevatedSurface,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share", color = PrimaryText) },
                                onClick = {
                                    menuOpen = false
                                    onShare()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (starred) "Unstar" else "Star", color = PrimaryText) },
                                onClick = {
                                    starred = !starred
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp).clip(RoundedCornerShape(28.dp))
                        .background(Color(0xE6212A2F))
                        .clickable(onClick = onReply)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Reply", color = SecondaryText, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text("❤️", fontSize = 27.sp, modifier = Modifier.clickable { onReaction("❤️") })
                    Spacer(Modifier.width(22.dp))
                    Text("😂", fontSize = 27.sp, modifier = Modifier.clickable { onReaction("😂") })
                    Spacer(Modifier.width(18.dp))
                    IconButton(onClick = onMoreReaction) {
                        Icon(Icons.Rounded.EmojiEmotions, "More reactions", tint = PrimaryText, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

private fun viewerDateLabel(timestampMillis: Long): String {
    if (timestampMillis <= 0) return "Today"
    return SimpleDateFormat("d MMMM, h:mm a", Locale.getDefault()).format(Date(timestampMillis))
        .replace("AM", "am")
        .replace("PM", "pm")
}

@Composable
private fun MediaComposer(
    uris: List<String>,
    selectedIndex: Int,
    recipient: String,
    onSelect: (Int) -> Unit,
    onAddMore: () -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSend: (String, Int) -> Unit,
) {
    val context = LocalContext.current
    val currentIndex = selectedIndex.coerceIn(uris.indices)
    val currentUri = uris[currentIndex]
    var caption by rememberSaveable(currentUri) { mutableStateOf("") }
    var toolsMenuOpen by remember { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = currentUri) {
        value = withContext(Dispatchers.IO) { context.decodeImageUri(Uri.parse(currentUri)) }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black).imePadding()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Selected photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            } else {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = BrandGreen)
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ComposerTool(Icons.Rounded.Close, "Close", onDismiss)
                Spacer(Modifier.weight(1f))
                ComposerTool(Icons.Rounded.Description, "HD") { toolsMenuOpen = true }
                ComposerTool(Icons.Rounded.GridOn, "Crop") { toolsMenuOpen = true }
                ComposerTool(Icons.Rounded.EmojiEmotions, "Sticker") { toolsMenuOpen = true }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color(0xCC10161A))
                        .clickable { toolsMenuOpen = true },
                    contentAlignment = Alignment.Center,
                ) { Text("Aa", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Medium) }
                ComposerTool(Icons.Rounded.Edit, "Draw") { toolsMenuOpen = true }
            }
            if (toolsMenuOpen) {
                AlertDialog(
                    onDismissRequest = { toolsMenuOpen = false },
                    containerColor = ElevatedSurface,
                    title = { Text("Photo editing", color = PrimaryText) },
                    text = { Text("The selected photo will be sent at original composition.", color = SecondaryText) },
                    confirmButton = { TextButton(onClick = { toolsMenuOpen = false }) { Text("OK", color = BrandGreen) } },
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                    .background(Color(0xD90B1014)).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            ) {
                if (uris.size > 1) {
                    LazyRow(
                        Modifier.fillMaxWidth().height(58.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    ) {
                        items(uris.size) { index ->
                            ComposerThumbnail(
                                uri = uris[index],
                                selected = index == currentIndex,
                                onSelect = { onSelect(index) },
                                onRemove = { onRemove(index) },
                            )
                        }
                        item {
                            Box(
                                Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(ComponentSurface)
                                    .clickable(onClick = onAddMore),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Add, "Add more photos", tint = PrimaryText) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xEE151D21))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CameraAlt, null, tint = PrimaryText, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = caption,
                        onValueChange = { caption = it.take(MAX_TEXT_LENGTH) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText),
                        cursorBrush = SolidColor(BrandGreen),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (caption.isBlank()) Text("Add a caption…", color = SecondaryText, fontSize = 18.sp)
                                inner()
                            }
                        },
                    )
                }
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recipient,
                        color = PrimaryText,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                            .background(Color(0xEE151D21)).padding(horizontal = 16.dp, vertical = 11.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.size(56.dp).shadow(7.dp, CircleShape).clip(CircleShape).background(ActionWhite)
                            .clickable { onSend(caption, currentIndex) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Send, "Send photos", tint = AppBackground, modifier = Modifier.size(29.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ComposerTool(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color(0xCC10161A)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = PrimaryText, modifier = Modifier.size(25.dp)) }
}

@Composable
private fun ComposerThumbnail(uri: String, selected: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) { context.decodeImageUri(Uri.parse(uri)) }
    }
    Box(
        Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))
            .border(if (selected) 2.dp else 0.dp, if (selected) ActionWhite else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect),
    ) {
        bitmap?.let {
            Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Color(0xCC10161A)).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Remove photo", tint = PrimaryText, modifier = Modifier.size(15.dp)) }
        }
    }
}

private fun Context.decodeImageUri(uri: Uri): Bitmap? = runCatching {
    when (uri.scheme) {
        "file" -> BitmapFactory.decodeFile(requireNotNull(uri.path))
        else -> contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
    }
}.getOrNull()

private fun Context.createCameraCaptureUri(): Uri {
    val directory = File(filesDir, "camera_capture").apply { mkdirs() }
    val output = File(directory, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.files", output)
}

private fun Context.deleteCameraCapture(uri: Uri) {
    runCatching { contentResolver.delete(uri, null, null) }
}

private fun formatFileSize(bytes: Long?): String = when {
    bytes == null -> "Document"
    bytes >= 1_048_576 -> "%.1f MB".format(Locale.US, bytes / 1_048_576f)
    bytes >= 1_024 -> "%.1f KB".format(Locale.US, bytes / 1_024f)
    else -> "$bytes B"
}

@Composable
private fun ReactionSummary(
    summaries: List<ReactionSummaryUi>,
    ownReaction: String?,
) {
    val visible = summaries.filter { it.count > 0 }.take(MAX_VISIBLE_REACTION_TYPES)
    if (visible.isEmpty()) return
    val total = summaries.sumOf { it.count.coerceAtLeast(0) }
    val accessibilityLabel = visible.joinToString { "${it.emoji} ${it.count}" }
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .offset(y = (-5).dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ComponentSurface)
            .border(
                width = 1.dp,
                color = if (ownReaction != null) BrandGreen else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .semantics {
                contentDescription = "$total reactions: $accessibilityLabel"
            }
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEachIndexed { index, summary ->
            if (index > 0) Spacer(Modifier.width(2.dp))
            Text(summary.emoji, fontSize = 16.sp)
        }
        if (total > 1) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = total.toString(),
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private const val MAX_VISIBLE_REACTION_TYPES = 3

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
    onMore: () -> Unit,
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
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(32.dp),
                clip = false,
                ambientColor = Color(0x66000000),
                spotColor = Color(0x7A000000),
            )
            .width(360.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF20272B))
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        emojis.forEach { emoji ->
            val active = selectedReaction == emoji
            Box(
                Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(if (active) 50.dp else 44.dp)
                        .clip(CircleShape)
                        .background(if (active) Color(0xFF3E474D) else Color.Transparent)
                        .clickable { onReaction(emoji) }
                        .semantics { contentDescription = "React $emoji" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 31.sp)
                }
            }
        }
        Box(
            Modifier.weight(1f).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF6F7D84)).clickable(onClick = onMore)
                    .semantics { contentDescription = "More reactions" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "More reactions", tint = Color(0xFF10171B), modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun InteractiveVoiceMessage(
    message: MessageUi,
    outgoing: Boolean,
    contact: com.jp.whatsappclone.data.ContactUi,
    active: Boolean,
    playing: Boolean,
    playbackProgress: Float,
    playbackSpeed: Float,
    onPlay: () -> Unit,
    onSpeed: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (outgoing) {
            VoiceAvatar(contact, outgoing = true)
            Spacer(Modifier.width(7.dp))
        }
        Box(
            // The waveform owns a metadata row below it. Without this offset the play
            // control is centered against waveform + metadata and appears too low.
            Modifier.size(42.dp).offset(y = (-9).dp).clip(CircleShape).clickable(onClick = onPlay)
                .semantics { contentDescription = if (playing) "Pause voice message" else "Play voice message" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (playing) "Pause voice message" else "Play voice message",
                tint = SecondaryText,
                modifier = Modifier.size(34.dp),
            )
        }
        Column(Modifier.width(if (!outgoing && active) 165.dp else 177.dp)) {
            VoiceWaveform(playbackProgress, Modifier.fillMaxWidth().height(31.dp), showThumb = true)
            Row {
                val displayedSeconds = if (active) ((message.voiceSeconds ?: 0) * playbackProgress).roundToInt() else (message.voiceSeconds ?: 0)
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
            Spacer(Modifier.width(7.dp))
            if (active) {
                VoiceSpeedButton(speed = playbackSpeed, onClick = onSpeed)
            } else {
                VoiceAvatar(contact, outgoing = false)
            }
        }
    }
}

@Composable
private fun VoiceAvatar(contact: com.jp.whatsappclone.data.ContactUi, outgoing: Boolean) {
    Box(Modifier.size(42.dp)) {
        Avatar(contact, size = 42.dp)
        Icon(
            Icons.Rounded.Mic,
            contentDescription = null,
            tint = ReadBlue,
            modifier = Modifier.align(if (outgoing) Alignment.BottomEnd else Alignment.BottomStart)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (outgoing) OutgoingBubble else IncomingBubble)
                .padding(1.dp),
        )
    }
}

@Composable
private fun VoiceSpeedButton(speed: Float, onClick: () -> Unit) {
    Box(
            Modifier.width(58.dp).height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF20272B)).clickable(onClick = onClick)
                .semantics { contentDescription = "Playback speed" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (speed == 1f) "1×" else if (speed == 1.5f) "1.5×" else "2×",
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
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
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 7.dp, end = 8.dp, bottom = 5.dp),
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
            Icon(Icons.Rounded.EmojiEmotions, "Emoji", tint = PrimaryText, modifier = Modifier.padding(horizontal = 12.dp))
            Text("Emoji", color = PrimaryText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AttachmentSheet(
    galleryAccessGranted: Boolean,
    onDismiss: () -> Unit,
    onRequestGalleryAccess: () -> Unit,
    onMediaSelected: (List<GalleryImage>) -> Unit,
    onBrowsePhotos: () -> Unit,
    onAction: (String) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var galleryExpanded by rememberSaveable { mutableStateOf(false) }
    var albumMenuOpen by remember { mutableStateOf(false) }
    var selectedAlbum by rememberSaveable { mutableStateOf("Recents") }
    var selectedMediaIds by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    val allMedia by produceState(
        initialValue = emptyList<GalleryImage>(),
        key1 = galleryAccessGranted,
    ) {
        value = if (galleryAccessGranted) withContext(Dispatchers.IO) {
            context.loadGalleryImages("Recents")
        } else {
            emptyList()
        }
    }
    val albums = remember(allMedia) {
        buildList {
            allMedia.firstOrNull()?.let { cover -> add(GalleryAlbum("Recents", allMedia.size, cover)) }
            allMedia.groupBy(GalleryImage::album)
                .filterKeys(String::isNotBlank)
                .forEach { (name, images) -> add(GalleryAlbum(name, images.size, images.first())) }
        }
    }
    val media = remember(allMedia, selectedAlbum) {
        if (selectedAlbum == "Recents") allMedia else allMedia.filter { it.album == selectedAlbum }
    }
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
        val expanded = galleryExpanded
        val sheetHeight = if (expanded) Modifier.fillMaxHeight(.93f) else Modifier.height(360.dp)
        val expandGesture = if (!expanded) {
            Modifier.pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -8f) galleryExpanded = true
                }
            }
        } else {
            Modifier
        }
        Column(
            Modifier.fillMaxWidth().then(sheetHeight)
                .navigationBarsPadding()
                .then(expandGesture),
        ) {
            if (expanded) {
                Row(
                    Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, "Close gallery", tint = PrimaryText)
                    }
                    Box {
                        Row(
                            Modifier.clip(RoundedCornerShape(12.dp)).clickable { albumMenuOpen = true }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(selectedAlbum, color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(6.dp))
                            Text("▾", color = PrimaryText, fontSize = 18.sp)
                        }
                        DropdownMenu(
                            expanded = albumMenuOpen,
                            onDismissRequest = { albumMenuOpen = false },
                            containerColor = ElevatedSurface,
                            modifier = Modifier.width(238.dp),
                        ) {
                            albums.forEach { album ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(album.name, color = PrimaryText, fontSize = 16.sp)
                                            Text("${album.count.toDisplayCount()} items", color = SecondaryText, fontSize = 12.sp)
                                        }
                                    },
                                    leadingIcon = {
                                        GalleryThumbnail(
                                            album.cover,
                                            Modifier.size(42.dp),
                                            onClick = {
                                                selectedAlbum = album.name
                                                albumMenuOpen = false
                                            },
                                        )
                                    },
                                    trailingIcon = {
                                        if (selectedAlbum == album.name) {
                                            Icon(Icons.Rounded.Check, null, tint = PrimaryText)
                                        }
                                    },
                                    onClick = {
                                        selectedAlbum = album.name
                                        albumMenuOpen = false
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("More apps", color = PrimaryText, fontSize = 16.sp)
                                        Text("Files, Gallery", color = SecondaryText, fontSize = 12.sp)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Rounded.Folder, null, tint = BrandGreen) },
                                trailingIcon = { Icon(Icons.Rounded.ChevronRight, null, tint = SecondaryText) },
                                onClick = {
                                    albumMenuOpen = false
                                    onBrowsePhotos()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("See more", color = PrimaryText, fontSize = 16.sp) },
                                leadingIcon = { Icon(Icons.Rounded.GridOn, null, tint = PrimaryText) },
                                onClick = {
                                    albumMenuOpen = false
                                    onBrowsePhotos()
                                },
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.width(40.dp).height(28.dp).clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF0F9E59)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("HD", color = AppBackground, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    GalleryGrid(
                        media = media,
                        galleryAccessGranted = galleryAccessGranted,
                        onRequestGalleryAccess = onRequestGalleryAccess,
                        selectedIds = selectedMediaIds,
                        onToggleMedia = { image ->
                            selectedMediaIds = if (image.id in selectedMediaIds) {
                                selectedMediaIds - image.id
                            } else {
                                (selectedMediaIds + image.id).take(30)
                            }
                        },
                        onBrowsePhotos = onBrowsePhotos,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (selectedMediaIds.isNotEmpty()) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).padding(18.dp).size(58.dp)
                                .shadow(8.dp, CircleShape).clip(CircleShape).background(BrandGreen)
                                .clickable {
                                    val selected = selectedMediaIds.mapNotNull { id -> allMedia.firstOrNull { it.id == id } }
                                    onMediaSelected(selected)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Send, "Continue with selected photos", tint = AppBackground, modifier = Modifier.size(27.dp))
                            Box(
                                Modifier.align(Alignment.TopEnd).size(21.dp).clip(CircleShape).background(ActionWhite),
                                contentAlignment = Alignment.Center,
                            ) { Text(selectedMediaIds.size.toString(), color = AppBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            } else {
                actions.chunked(4).forEach { rowActions ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        rowActions.forEach { (label, icon, color) -> SheetAction(label, icon, color) { onAction(label) } }
                    }
                }
                if (galleryAccessGranted && media.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        items(media.take(8), key = GalleryImage::id) { image ->
                            GalleryThumbnail(image, Modifier.width(92.dp).height(128.dp)) {
                                onMediaSelected(listOf(image))
                            }
                        }
                    }
                } else {
                    GalleryAccessPlaceholder(
                        onRequestGalleryAccess = onRequestGalleryAccess,
                        onBrowsePhotos = onBrowsePhotos,
                    )
                }
            }
        }
    }
}

private data class GalleryImage(val id: Long, val uri: Uri, val album: String)

private data class GalleryAlbum(val name: String, val count: Int, val cover: GalleryImage)

private fun Int.toDisplayCount(): String = java.text.NumberFormat.getIntegerInstance().format(this)

@Composable
private fun GalleryGrid(
    media: List<GalleryImage>,
    galleryAccessGranted: Boolean,
    onRequestGalleryAccess: () -> Unit,
    selectedIds: List<Long>,
    onToggleMedia: (GalleryImage) -> Unit,
    onBrowsePhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!galleryAccessGranted || media.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GalleryAccessPlaceholder(onRequestGalleryAccess, onBrowsePhotos)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        gridItems(media, key = GalleryImage::id) { image ->
            val order = selectedIds.indexOf(image.id).takeIf { it >= 0 }?.plus(1)
            GalleryThumbnail(image, Modifier.aspectRatio(1f), selectedOrder = order) { onToggleMedia(image) }
        }
    }
}

@Composable
private fun GalleryThumbnail(image: GalleryImage, modifier: Modifier, selectedOrder: Int? = null, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = image.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(image.uri, AndroidSize(360, 360), null)
                } else {
                    context.contentResolver.openInputStream(image.uri)?.use(BitmapFactory::decodeStream)
                }
            }.getOrNull()
        }
    }
    Box(
        modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFF172126)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Gallery image",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            CircularProgressIndicator(Modifier.size(20.dp), color = BrandGreen, strokeWidth = 2.dp)
        }
        if (selectedOrder != null) {
            Box(Modifier.matchParentSize().border(3.dp, BrandGreen, RoundedCornerShape(3.dp)))
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(BrandGreen),
                contentAlignment = Alignment.Center,
            ) { Text(selectedOrder.toString(), color = AppBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun GalleryAccessPlaceholder(onRequestGalleryAccess: () -> Unit, onBrowsePhotos: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onRequestGalleryAccess) { Text("Show recent photos", color = BrandGreen) }
        TextButton(onClick = onBrowsePhotos) { Text("Choose other…", color = PrimaryText) }
    }
}

private fun galleryImagePermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun Context.hasGalleryImageAccess(): Boolean = galleryImagePermissions().any { permission ->
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Context.loadGalleryImages(album: String): List<GalleryImage> {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
    )
    val selection = album.takeUnless { it == "Recents" }?.let {
        "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
    }
    val selectionArgs: Array<String>? = album.takeUnless { it == "Recents" }?.let { value -> arrayOf(value) }
    val images = mutableListOf<GalleryImage>()
    contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Images.Media.DATE_ADDED} DESC",
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            images += GalleryImage(
                id = id,
                uri = Uri.withAppendedPath(collection, id.toString()),
                album = cursor.getString(albumColumn).orEmpty(),
            )
        }
    }
    return images
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

private fun sameLocalDay(firstMillis: Long, secondMillis: Long): Boolean {
    if (firstMillis <= 0 || secondMillis <= 0) return firstMillis <= 0 && secondMillis <= 0
    val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
    return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

private fun dateHeaderLabel(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (timestampMillis <= 0) return "Today"
    if (sameLocalDay(timestampMillis, nowMillis)) return "Today"
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis
    if (sameLocalDay(timestampMillis, yesterday)) return "Yesterday"
    return SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestampMillis))
}

private fun restingVoiceProgress(messageId: String): Float = when (messageId) {
    "m4" -> .08f
    else -> .04f
}

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

private fun android.content.Context.resolveAttachmentMetadata(uri: Uri): Pair<String, Long?> {
    var name: String? = null
    var size: Long? = null
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
    return (name?.takeIf { it.isNotBlank() } ?: "attachment") to size
}
