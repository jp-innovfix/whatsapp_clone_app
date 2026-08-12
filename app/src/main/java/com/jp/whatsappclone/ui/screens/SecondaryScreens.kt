package com.jp.whatsappclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GifBox
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jp.whatsappclone.data.AppAction
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.data.ContactUi
import com.jp.whatsappclone.data.filteredSearchChats
import com.jp.whatsappclone.ui.ActionWhite
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.BrandGreen
import com.jp.whatsappclone.ui.ComponentSurface
import com.jp.whatsappclone.ui.DividerColor
import com.jp.whatsappclone.ui.ElevatedSurface
import com.jp.whatsappclone.ui.PrimaryText
import com.jp.whatsappclone.ui.SecondaryText
import com.jp.whatsappclone.ui.components.ArchivedRow
import com.jp.whatsappclone.ui.components.Avatar
import com.jp.whatsappclone.ui.components.ChatRow

@Composable
fun SearchScreen(
    state: AppState,
    dispatch: (AppAction) -> Unit,
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    showMessage: (String) -> Unit,
    autoFocus: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    val results = state.filteredSearchChats()
    Column(Modifier.fillMaxSize().background(AppBackground).imePadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(66.dp).padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.fillMaxWidth().height(44.dp).offset(y = (-7).dp).clip(RoundedCornerShape(24.dp)).background(ElevatedSurface),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = { dispatch(AppAction.SetSearchQuery(it)) },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText),
                    cursorBrush = SolidColor(BrandGreen),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (state.searchQuery.isBlank()) Text("Search…", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(12.dp))
            }
        }
        SearchFilterLine(
            listOf("Photos" to Icons.Rounded.Image, "Videos" to Icons.Rounded.VideoFile, "Links" to Icons.Rounded.Link),
            state,
            dispatch,
        )
        SearchFilterLine(
            listOf("GIFs" to Icons.Rounded.GifBox, "Audio" to Icons.Rounded.AudioFile, "Documents" to Icons.Rounded.Description),
            state,
            dispatch,
        )
        SearchFilterLine(
            listOf("Stickers" to Icons.Rounded.StickyNote2, "Polls" to Icons.Rounded.Poll),
            state,
            dispatch,
        )
        Divider(color = DividerColor)
        SearchFilterLine(
            listOf("Contacts" to Icons.Rounded.Person, "Non-contacts" to Icons.Rounded.PersonAdd, "Unread" to Icons.Rounded.MarkChatUnread),
            state,
            dispatch,
        )
        Divider(color = DividerColor)
        LazyColumn(Modifier.fillMaxSize()) {
            item { ArchivedRow(28, Modifier.padding(top = 72.dp)) }
            items(results, key = { it.id }) { thread ->
                ChatRow(
                    thread = thread,
                    onClick = { onChat(thread.id) },
                    onAvatarClick = { showMessage("Contact preview is available from Chats") },
                )
            }
        }
    }
}

@Composable
private fun SearchFilterLine(
    filters: List<Pair<String, ImageVector>>,
    state: AppState,
    dispatch: (AppAction) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((label, icon) in filters) {
            val selected = label in state.searchFilters
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(18.dp)).background(if (selected) Color(0xFF16422F) else ComponentSurface)
                    .clickable { dispatch(AppAction.ToggleSearchFilter(label)) }.padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = if (selected) BrandGreen else SecondaryText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = PrimaryText, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ContactPickerScreen(
    state: AppState,
    onBack: () -> Unit,
    onPick: (ContactUi) -> Unit,
    showMessage: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
            Column(Modifier.weight(1f)) {
                Text("Select contact", color = PrimaryText, style = MaterialTheme.typography.titleMedium)
                Text("543 contacts", color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { showMessage("Contact search is mocked") }) { Icon(Icons.Rounded.Search, "Search", tint = PrimaryText) }
            IconButton(onClick = { showMessage("Contact options are mocked") }) { Icon(Icons.Rounded.MoreVert, "More", tint = PrimaryText) }
        }
        Divider(color = DividerColor)
        LazyColumn(Modifier.fillMaxSize()) {
            item { ContactShortcut(Icons.Rounded.GroupAdd, "New group") { showMessage("New group is mocked") } }
            item { ContactShortcut(Icons.Rounded.PersonAdd, "New contact", Icons.Rounded.QrCode2) { showMessage("New contact is mocked") } }
            item { ContactShortcut(Icons.Rounded.ContactPage, "New business broadcast") { showMessage("Business broadcast is mocked") } }
            item { Text("Contacts on WhatsApp", color = SecondaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) }
            items(state.contacts, key = { it.id }) { contact ->
                Row(
                    Modifier.fillMaxWidth().height(64.dp).clickable { onPick(contact) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(contact, size = 38.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(contact.name, color = PrimaryText, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (contact.id == "own") Text("Message yourself", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactShortcut(icon: ImageVector, label: String, trailing: ImageVector? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(70.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(ActionWhite), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AppBackground, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = PrimaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.let { Icon(it, null, tint = PrimaryText) }
    }
}

@Composable
fun NotificationsScreen(
    state: AppState,
    dispatch: (AppAction) -> Unit,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().background(AppBackground)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
                Text("Notifications", color = PrimaryText, style = MaterialTheme.typography.headlineLarge)
            }
        }
        item {
            SettingSwitchRow("Mute notifications", state.messageNotificationsMuted) { dispatch(AppAction.ToggleMessageMute) }
            SettingRow("Notification tone", "Default (Spaceline)") { showMessage("Tone picker is mocked") }
            SettingRow("Vibrate", "Default") { showMessage("Vibration setting is mocked") }
            SettingRow("Advanced settings", null) { showMessage("Advanced settings are mocked") }
            Divider(color = DividerColor)
            Text("Call", color = SecondaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            SettingRow("Ringtone", "Default (ReelAudio-79918)") { showMessage("Ringtone picker is mocked") }
            SettingRow("Vibrate", "Default") { showMessage("Call vibration is mocked") }
            Divider(color = DividerColor)
            Text("Status", color = SecondaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            SettingSwitchRow("Mute notifications", state.statusNotificationsMuted) { dispatch(AppAction.ToggleStatusMute) }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 18.dp)) {
        Text(title, color = PrimaryText, style = MaterialTheme.typography.titleMedium)
        subtitle?.let { Text(it, color = SecondaryText, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = PrimaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppBackground,
                checkedTrackColor = ActionWhite,
                uncheckedThumbColor = SecondaryText,
                uncheckedTrackColor = ComponentSurface,
                uncheckedBorderColor = SecondaryText,
            ),
        )
    }
}
