package com.jp.whatsappclone

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jp.whatsappclone.presentation.AuthViewModel
import com.jp.whatsappclone.presentation.HomeViewModel
import com.jp.whatsappclone.data.domain.SessionState
import com.jp.whatsappclone.ui.WhatsAppApp
import com.jp.whatsappclone.ui.WhatsAppCloneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLinkedConversationId by mutableStateOf<String?>(null)
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkedConversationId = intent.conversationId()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK),
        )
        setContent {
            WhatsAppCloneTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val homeViewModel: HomeViewModel = hiltViewModel()
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(authState.session) {
                    if (authState.session is SessionState.SignedIn &&
                        android.os.Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                WhatsAppApp(
                    authState = authState,
                    homeState = homeState,
                    dispatch = homeViewModel::dispatch,
                    onEnsureAnonymousSession = authViewModel::ensureAnonymousSession,
                    onSignOut = authViewModel::signOut,
                    openDirectConversation = homeViewModel::openDirectConversation,
                    createGroup = homeViewModel::createGroup,
                    forwardMessage = homeViewModel::forwardMessage,
                    onHomeErrorConsumed = homeViewModel::clearError,
                    onExit = { finish() },
                    initialConversationId = deepLinkedConversationId,
                    onInitialConversationConsumed = { deepLinkedConversationId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkedConversationId = intent.conversationId()
    }

    private fun Intent.conversationId(): String? =
        getStringExtra(EXTRA_CONVERSATION_ID)?.takeIf(String::isNotBlank)

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversationId"
    }
}
