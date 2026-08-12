package com.jp.whatsappclone.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.jp.whatsappclone.data.AppAction
import com.jp.whatsappclone.data.AppDestination
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.ui.screens.ChatScreen
import com.jp.whatsappclone.ui.screens.ContactPickerScreen
import com.jp.whatsappclone.ui.screens.GroupInfoScreen
import com.jp.whatsappclone.ui.screens.LoginScreen
import com.jp.whatsappclone.ui.screens.MainScreen
import com.jp.whatsappclone.ui.screens.NotificationsScreen
import com.jp.whatsappclone.ui.screens.SearchScreen
import kotlinx.coroutines.launch

@Composable
fun WhatsAppApp(
    state: AppState,
    dispatch: (AppAction) -> Unit,
) {
    val backStack = rememberNavBackStack(AppDestination.Login)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
    val openThreadForContact: (String) -> Unit = { contactId ->
        val thread = state.chats.firstOrNull { it.contact.id == contactId }
        if (thread != null) backStack.add(AppDestination.Chat(thread.id))
        else showMessage("A mock conversation would be created here")
    }

    Box(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
            entryProvider = entryProvider {
                entry<AppDestination.Login> {
                    LoginScreen(
                        onLogin = {
                            backStack.removeLastOrNull()
                            backStack.add(AppDestination.Main)
                        },
                    )
                }
                entry<AppDestination.Main> {
                    MainScreen(
                        state = state,
                        dispatch = dispatch,
                        onOpenChat = { backStack.add(AppDestination.Chat(it)) },
                        onSearch = { backStack.add(AppDestination.Search) },
                        onContactPicker = { backStack.add(AppDestination.ContactPicker) },
                        onNotifications = { backStack.add(AppDestination.Notifications) },
                        showMessage = showMessage,
                    )
                }
                entry<AppDestination.Chat> { destination ->
                    ChatScreen(
                        threadId = destination.threadId,
                        state = state,
                        dispatch = dispatch,
                        onBack = goBack,
                        onInfo = {
                            val thread = state.chats.firstOrNull { it.id == destination.threadId }
                            if (thread?.contact?.isGroup == true) backStack.add(AppDestination.GroupInfo(destination.threadId))
                            else showMessage("Contact info is mocked")
                        },
                        showMessage = showMessage,
                    )
                }
                entry<AppDestination.GroupInfo> { destination ->
                    GroupInfoScreen(
                        threadId = destination.threadId,
                        state = state,
                        onBack = goBack,
                        showMessage = showMessage,
                    )
                }
                entry<AppDestination.Search> {
                    SearchScreen(
                        state = state,
                        dispatch = dispatch,
                        onBack = goBack,
                        onChat = { backStack.add(AppDestination.Chat(it)) },
                        showMessage = showMessage,
                    )
                }
                entry<AppDestination.ContactPicker> {
                    ContactPickerScreen(
                        state = state,
                        onBack = goBack,
                        onPick = { openThreadForContact(it.id) },
                        showMessage = showMessage,
                    )
                }
                entry<AppDestination.Notifications> {
                    NotificationsScreen(
                        state = state,
                        dispatch = dispatch,
                        onBack = goBack,
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
