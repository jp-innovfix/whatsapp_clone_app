package com.jp.whatsappclone.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.data.MainTab
import com.jp.whatsappclone.data.MockBusinessRepository
import com.jp.whatsappclone.data.RecordingUi
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.WhatsAppCloneTheme
import com.jp.whatsappclone.ui.screens.ChatScreen
import com.jp.whatsappclone.ui.screens.ContactPickerScreen
import com.jp.whatsappclone.ui.screens.MainScreen
import com.jp.whatsappclone.ui.screens.NotificationsScreen
import com.jp.whatsappclone.ui.screens.SearchScreen

private fun demoState(): AppState = MockBusinessRepository().appState.value

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    WhatsAppCloneTheme(darkTheme = true) {
        Box(Modifier.fillMaxSize().background(AppBackground)) { content() }
    }
}

@Composable
private fun MainReference(
    state: AppState,
    previewContactId: String? = null,
    overflow: Boolean = false,
    toolsStartIndex: Int = 0,
) = PreviewFrame {
    MainScreen(
        state = state,
        dispatch = {},
        onOpenChat = {},
        onSearch = {},
        onContactPicker = {},
        onNotifications = {},
        showMessage = {},
        initialPreviewContactId = previewContactId,
        initialOverflowMenu = overflow,
        toolsStartIndex = toolsStartIndex,
    )
}

@Preview(name = "01 Conversation", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConversationPreview() = PreviewFrame {
    ChatScreen("chat_mia", demoState(), {}, {}, {})
}

@Preview(name = "02 Chats home", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatsPreview() = MainReference(demoState())

@Preview(name = "03 Contact preview", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactPreview() = MainReference(demoState(), previewContactId = "dev")

@Preview(name = "04 Tools lower", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToolsLowerPreview() = MainReference(demoState().copy(selectedTab = MainTab.Tools), toolsStartIndex = 10)

@Preview(name = "05 Chats overflow", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatsOverflowPreview() = MainReference(demoState(), overflow = true)

@Preview(name = "06 Calls", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CallsPreview() = MainReference(demoState().copy(selectedTab = MainTab.Calls))

@Preview(name = "07 Updates", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpdatesPreview() = MainReference(demoState().copy(selectedTab = MainTab.Updates))

@Preview(name = "08 Tools top", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToolsTopPreview() = MainReference(demoState().copy(selectedTab = MainTab.Tools))

@Preview(name = "09 Attachment sheet", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttachmentPreview() = PreviewFrame {
    ChatScreen("chat_mia", demoState(), {}, {}, {}, initialAttachmentSheet = true)
}

@Preview(name = "10 Focused composer", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FocusedComposerPreview() = PreviewFrame {
    ChatScreen("chat_mia", demoState(), {}, {}, {}, initialComposerText = "Sounds good — sending this now")
}

@Preview(name = "11 Notifications", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotificationsPreview() = PreviewFrame {
    NotificationsScreen(demoState(), {}, {}, {})
}

@Preview(name = "12 Global search", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchPreview() = PreviewFrame {
    SearchScreen(demoState().copy(searchQuery = "Mia"), {}, {}, {}, {}, autoFocus = false)
}

@Preview(name = "13 Contact picker", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContactPickerPreview() = PreviewFrame {
    ContactPickerScreen(demoState(), {}, {}, {})
}

@Preview(name = "14 Voice recording", widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecordingPreview() = PreviewFrame {
    ChatScreen("chat_mia", demoState().copy(recording = RecordingUi(active = true, seconds = 13)), {}, {}, {})
}
