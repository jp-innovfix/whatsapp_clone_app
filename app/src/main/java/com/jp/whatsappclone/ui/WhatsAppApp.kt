package com.jp.whatsappclone.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.jp.whatsappclone.data.AppAction
import com.jp.whatsappclone.data.AppDestination
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.data.ChatThreadUi
import com.jp.whatsappclone.data.ContactUi
import com.jp.whatsappclone.data.domain.SessionState
import com.jp.whatsappclone.presentation.AuthUiState
import com.jp.whatsappclone.presentation.ChatViewModel
import com.jp.whatsappclone.presentation.HomeUiState
import com.jp.whatsappclone.ui.screens.ChatScreen
import com.jp.whatsappclone.ui.screens.ContactPickerScreen
import com.jp.whatsappclone.ui.screens.GroupInfoScreen
import com.jp.whatsappclone.ui.screens.MainScreen
import com.jp.whatsappclone.ui.screens.NotificationsScreen
import com.jp.whatsappclone.ui.screens.NewGroupScreen
import com.jp.whatsappclone.ui.screens.ForwardPickerScreen
import com.jp.whatsappclone.ui.screens.SearchScreen
import kotlinx.coroutines.launch

@Composable
fun WhatsAppApp(
    authState: AuthUiState,
    homeState: HomeUiState,
    dispatch: (AppAction) -> Unit,
    onEnsureAnonymousSession: () -> Unit,
    onSignOut: () -> Unit,
    openDirectConversation: suspend (String) -> Result<String>,
    createGroup: suspend (String, List<String>) -> Result<String>,
    forwardMessage: suspend (String, String, String) -> Result<String>,
    onHomeErrorConsumed: () -> Unit,
    onExit: () -> Unit,
    initialConversationId: String? = null,
    onInitialConversationConsumed: () -> Unit = {},
) {
    when (val session = authState.session) {
        SessionState.Loading -> AnonymousStartScreen(authState, onEnsureAnonymousSession)
        SessionState.SignedOut -> {
            LaunchedEffect(Unit) { onEnsureAnonymousSession() }
            AnonymousStartScreen(authState, onEnsureAnonymousSession)
        }
        is SessionState.Disabled -> AnonymousStartScreen(
            authState.copy(errorMessage = session.reason ?: authState.errorMessage),
            onEnsureAnonymousSession,
        )
        is SessionState.SignedIn -> key(session.user.id) {
            MessengerNavigation(
                state = homeState.appState,
                dispatch = dispatch,
                onSignOut = onSignOut,
                openDirectConversation = openDirectConversation,
                createGroup = createGroup,
                forwardMessage = forwardMessage,
                homeErrorMessage = homeState.errorMessage,
                onHomeErrorConsumed = onHomeErrorConsumed,
                onExit = onExit,
                initialConversationId = initialConversationId,
                onInitialConversationConsumed = onInitialConversationConsumed,
            )
        }
    }
}

@Composable
private fun MessengerNavigation(
    state: AppState,
    dispatch: (AppAction) -> Unit,
    onSignOut: () -> Unit,
    openDirectConversation: suspend (String) -> Result<String>,
    createGroup: suspend (String, List<String>) -> Result<String>,
    forwardMessage: suspend (String, String, String) -> Result<String>,
    homeErrorMessage: String?,
    onHomeErrorConsumed: () -> Unit,
    onExit: () -> Unit,
    initialConversationId: String?,
    onInitialConversationConsumed: () -> Unit,
) {
    val backStack = rememberNavBackStack(AppDestination.Main)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var openingContactId by remember { mutableStateOf<String?>(null) }
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull() else onExit()
    }

    fun navigateToChat(conversationId: String, title: String? = null, contactId: String? = null) {
        if ((backStack.lastOrNull() as? AppDestination.Chat)?.threadId != conversationId) {
            backStack.add(AppDestination.Chat(conversationId, title, contactId))
        }
    }

    LaunchedEffect(homeErrorMessage) {
        homeErrorMessage?.let { message ->
            showMessage(message)
            onHomeErrorConsumed()
        }
    }

    val openThreadForContact: (ContactUi) -> Unit = openThread@{ contact ->
        val existing = state.chats.firstOrNull {
            it.contact.id == contact.id || (!it.contact.isGroup && it.contact.name == contact.name)
        }
        if (existing != null) {
            navigateToChat(existing.id, existing.contact.name, contact.id)
        } else {
            if (openingContactId == contact.id) return@openThread
            openingContactId = contact.id
            scope.launch {
                try {
                    openDirectConversation(contact.id)
                        .onSuccess { conversationId ->
                            navigateToChat(conversationId, contact.name, contact.id)
                        }
                } finally {
                    if (openingContactId == contact.id) openingContactId = null
                }
            }
        }
    }

    val authorizedConversationIds = state.chats.map { it.id }
    LaunchedEffect(initialConversationId, authorizedConversationIds) {
        val requestedId = initialConversationId?.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        val chat = state.chats.firstOrNull { it.id == requestedId } ?: return@LaunchedEffect
        val destination = AppDestination.Chat(requestedId, chat.contact.name, chat.contact.id)
        if ((backStack.lastOrNull() as? AppDestination.Chat)?.threadId != requestedId) {
            backStack.add(destination)
        }
        onInitialConversationConsumed()
    }

    Box(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = {
                (slideInHorizontally(tween(240)) { it } + fadeIn(tween(150))) togetherWith
                    (slideOutHorizontally(tween(240)) { -it / 4 } + fadeOut(tween(120)))
            },
            popTransitionSpec = {
                (slideInHorizontally(tween(240)) { -it / 4 } + fadeIn(tween(150))) togetherWith
                    (slideOutHorizontally(tween(240)) { it } + fadeOut(tween(120)))
            },
            entryProvider = entryProvider {
                entry<AppDestination.Login> {
                    // Authentication is rendered outside navigation and this route remains only for
                    // backward-compatible saved stacks created by earlier debug builds.
                    LoadingMessengerScreen()
                }
                entry<AppDestination.Main> {
                    MainScreen(
                        state = state,
                        dispatch = dispatch,
                        onOpenChat = { navigateToChat(it) },
                        onOpenContact = openThreadForContact,
                        onSearch = { backStack.add(AppDestination.Search) },
                        onContactPicker = { backStack.add(AppDestination.ContactPicker) },
                        onNotifications = { backStack.add(AppDestination.Notifications) },
                        onNewGroup = { backStack.add(AppDestination.NewGroup) },
                        showMessage = showMessage,
                    )
                }
                entry<AppDestination.Chat> { destination ->
                    val chatViewModel: ChatViewModel = hiltViewModel()
                    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
                    val chatMessages = chatViewModel.messages.collectAsLazyPagingItems()
                    LaunchedEffect(destination.threadId) { chatViewModel.bind(destination.threadId) }
                    DisposableEffect(destination.threadId) {
                        onDispose(chatViewModel::unbind)
                    }
                    LaunchedEffect(chatState.errorMessage) {
                        chatState.errorMessage?.let {
                            showMessage(it)
                            chatViewModel.clearError()
                        }
                    }

                    val sourceThread = state.chats.firstOrNull { it.id == destination.threadId }
                    val placeholderContact = destination.contactId
                        ?.let { id -> state.contacts.firstOrNull { it.id == id } }
                        ?: sourceThread?.contact
                        ?: ContactUi(
                            id = destination.contactId ?: destination.threadId,
                            name = destination.title ?: "Conversation",
                            initials = (destination.title ?: "Conversation").initialsForUi(),
                            color = 0xFF31576E,
                        )
                    val liveThread = sourceThread ?: ChatThreadUi(
                        id = destination.threadId,
                        contact = placeholderContact,
                        preview = "",
                        time = "",
                    )
                    val chatScreenState = if (sourceThread == null) {
                        state.copy(chats = state.chats + liveThread)
                    } else {
                        state
                    }
                    val chatDispatch: (AppAction) -> Unit = { action ->
                        when (action) {
                            is AppAction.SendText,
                            is AppAction.SendAttachment,
                            is AppAction.RetryMessage,
                            is AppAction.SetMessageReaction,
                            is AppAction.DeleteMessage
                            -> chatViewModel.dispatch(action)
                            else -> dispatch(action)
                        }
                    }
                    ChatScreen(
                        threadId = destination.threadId,
                        state = chatScreenState,
                        pagedMessages = chatMessages,
                        dispatch = chatDispatch,
                        onBack = goBack,
                        onInfo = {
                            if (liveThread.contact.isGroup) {
                                backStack.add(AppDestination.GroupInfo(destination.threadId))
                            } else {
                                showMessage(V1_UNAVAILABLE_MESSAGE)
                            }
                        },
                        showMessage = showMessage,
                        persistedDraft = chatState.draft,
                        onDraftChanged = chatViewModel::onDraftChanged,
                        typingMemberNames = chatState.typingMemberNames,
                        onLoadOlder = chatViewModel::loadOlder,
                        hasMoreHistory = chatState.hasMoreHistory,
                        historyLoading = chatState.loadingOlder,
                        onNewestMessageVisible = chatViewModel::markVisible,
                        onForwardMessage = { messageId ->
                            backStack.add(AppDestination.ForwardMessage(destination.threadId, messageId))
                        },
                        onPrepareAttachment = chatViewModel::prepareAttachment,
                    )
                }
                entry<AppDestination.ActiveCall> {
                    LaunchedEffect(Unit) {
                        showMessage(V1_UNAVAILABLE_MESSAGE)
                        goBack()
                    }
                    LoadingMessengerScreen()
                }
                entry<AppDestination.CallResult> {
                    LaunchedEffect(Unit) {
                        showMessage(V1_UNAVAILABLE_MESSAGE)
                        goBack()
                    }
                    LoadingMessengerScreen()
                }
                entry<AppDestination.GroupInfo> { destination ->
                    GroupInfoScreen(
                        threadId = destination.threadId,
                        state = state,
                        dispatch = dispatch,
                        onBack = goBack,
                        showMessage = showMessage,
                    )
                }
                entry<AppDestination.NewGroup> {
                    NewGroupScreen(
                        state = state,
                        onBack = goBack,
                        onCreate = { title, memberIds ->
                            scope.launch {
                                createGroup(title, memberIds).onSuccess { conversationId ->
                                    backStack.removeLastOrNull()
                                    navigateToChat(conversationId, title)
                                }.onFailure { showMessage(it.message ?: "Unable to create group") }
                            }
                        },
                    )
                }
                entry<AppDestination.ForwardMessage> { destination ->
                    ForwardPickerScreen(
                        state = state,
                        sourceConversationId = destination.conversationId,
                        onBack = goBack,
                        onPick = { targetConversationId ->
                            scope.launch {
                                forwardMessage(destination.conversationId, destination.messageId, targetConversationId)
                                    .onSuccess {
                                        backStack.removeLastOrNull()
                                        showMessage("Message forwarded")
                                    }
                                    .onFailure { showMessage(it.message ?: "Unable to forward message") }
                            }
                        },
                    )
                }
                entry<AppDestination.Search> {
                    SearchScreen(
                        state = state,
                        dispatch = dispatch,
                        onBack = goBack,
                        onChat = { navigateToChat(it) },
                        showMessage = showMessage,
                        onContact = openThreadForContact,
                    )
                }
                entry<AppDestination.ContactPicker> {
                    ContactPickerScreen(
                        state = state,
                        onBack = goBack,
                        onPick = openThreadForContact,
                        showMessage = showMessage,
                        onNewGroup = { backStack.add(AppDestination.NewGroup) },
                    )
                }
                entry<AppDestination.Notifications> {
                    NotificationsScreen(
                        state = state,
                        dispatch = dispatch,
                        onBack = goBack,
                        onSignOut = onSignOut,
                        showMessage = showMessage,
                    )
                }
            },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),
        ) { data ->
            Snackbar(containerColor = ComponentSurface, contentColor = PrimaryText) {
                Text(data.visuals.message)
            }
        }
    }
}

@Composable
private fun LoadingMessengerScreen() {
    Box(Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandGreen)
    }
}

@Composable
private fun AnonymousStartScreen(authState: AuthUiState, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = BrandGreen)
            authState.errorMessage?.let { error ->
                Spacer(Modifier.height(20.dp))
                Text(error, color = PrimaryText, modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry, enabled = !authState.submitting) { Text("Retry") }
            }
        }
    }
}

private fun String.initialsForUi(): String = trim().split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
    .joinToString("")
    .ifBlank { "?" }

private const val V1_UNAVAILABLE_MESSAGE = "Not available in this version"
