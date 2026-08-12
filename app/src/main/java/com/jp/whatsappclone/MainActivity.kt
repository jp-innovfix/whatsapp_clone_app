package com.jp.whatsappclone

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jp.whatsappclone.data.AppViewModel
import com.jp.whatsappclone.ui.WhatsAppApp
import com.jp.whatsappclone.ui.WhatsAppCloneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK),
        )
        setContent {
            WhatsAppCloneTheme {
                val appViewModel: AppViewModel = viewModel()
                val state = appViewModel.appState.collectAsStateWithLifecycle().value
                WhatsAppApp(state = state, dispatch = appViewModel::dispatch)
            }
        }
    }
}
