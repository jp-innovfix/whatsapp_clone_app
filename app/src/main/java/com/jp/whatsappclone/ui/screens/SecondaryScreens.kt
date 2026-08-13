package com.jp.whatsappclone.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.jp.whatsappclone.data.filteredSearchContacts
import com.jp.whatsappclone.ui.ActionWhite
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.BrandGreen
import com.jp.whatsappclone.ui.ComponentSurface
import com.jp.whatsappclone.ui.DividerColor
import com.jp.whatsappclone.ui.ElevatedSurface
import com.jp.whatsappclone.ui.PrimaryText
import com.jp.whatsappclone.ui.SecondaryText
import com.jp.whatsappclone.ui.components.Avatar
import com.jp.whatsappclone.ui.components.ChatRow

@Composable
fun SearchScreen(
    state: AppState,
    dispatch: (AppAction) -> Unit,
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    showMessage: (String) -> Unit,
    onContact: (ContactUi) -> Unit = {},
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
    val contactResults = state.filteredSearchContacts()
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
            listOf("Contacts" to Icons.Rounded.Person, "Groups" to Icons.Rounded.GroupAdd, "Unread" to Icons.Rounded.MarkChatUnread),
            state,
            dispatch,
        )
        Divider(color = DividerColor)
        LazyColumn(Modifier.fillMaxSize()) {
            if (contactResults.isNotEmpty()) {
                item {
                    Text(
                        "Contacts",
                        color = SecondaryText,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                items(contactResults, key = { "search-contact:${it.id}" }) { contact ->
                    Row(
                        Modifier.fillMaxWidth().height(64.dp).clickable { onContact(contact) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(contact, size = 38.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                contact.name,
                                color = PrimaryText,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                contact.subtitle,
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            items(results, key = { it.id }) { thread ->
                ChatRow(
                    thread = thread,
                    onClick = { onChat(thread.id) },
                    onAvatarClick = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                    onLongClick = { showMessage(V1_UNAVAILABLE_MESSAGE) },
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
    onNewGroup: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
            Column(Modifier.weight(1f)) {
                Text("Select contact", color = PrimaryText, style = MaterialTheme.typography.titleMedium)
                Text("${state.contacts.size} employees", color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { showMessage(V1_UNAVAILABLE_MESSAGE) }) { Icon(Icons.Rounded.Search, "Search", tint = PrimaryText) }
            IconButton(onClick = { showMessage(V1_UNAVAILABLE_MESSAGE) }) { Icon(Icons.Rounded.MoreVert, "More", tint = PrimaryText) }
        }
        Divider(color = DividerColor)
        LazyColumn(Modifier.fillMaxSize()) {
            item { ContactShortcut(Icons.Rounded.GroupAdd, "New group", onClick = onNewGroup) }
            item { ContactShortcut(Icons.Rounded.PersonAdd, "New contact", Icons.Rounded.QrCode2) { showMessage(V1_UNAVAILABLE_MESSAGE) } }
            item { ContactShortcut(Icons.Rounded.ContactPage, "New business broadcast") { showMessage(V1_UNAVAILABLE_MESSAGE) } }
            item { Text("INNOVFIX employees", color = SecondaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) }
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
fun NewGroupScreen(
    state: AppState,
    onBack: () -> Unit,
    onCreate: (String, List<String>) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var namingStep by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val contacts = remember(state.contacts, query) {
        state.contacts.filter { contact ->
            contact.id != "own" && (query.isBlank() || contact.name.contains(query, ignoreCase = true) || contact.subtitle.contains(query, ignoreCase = true))
        }
    }
    fun requestBack() {
        when {
            namingStep -> namingStep = false
            selectedIds.isNotEmpty() -> showDiscardDialog = true
            else -> onBack()
        }
    }
    BackHandler(onBack = ::requestBack)
    LaunchedEffect(namingStep) {
        if (namingStep) {
            nameFocusRequester.requestFocus()
            keyboard?.show()
        }
    }
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        if (!namingStep) Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(68.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(26.dp)).background(ElevatedSurface),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::requestBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it.take(80) },
                        modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText),
                        cursorBrush = SolidColor(BrandGreen),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isBlank()) Text("Name, number, @username", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
                                inner()
                            }
                        },
                    )
                    Icon(Icons.Rounded.QrCode2, "Scan QR code", tint = PrimaryText, modifier = Modifier.padding(end = 14.dp).size(27.dp))
                }
            }
            if (selectedIds.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().height(102.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    selectedIds.forEach { id ->
                        val contact = state.contacts.firstOrNull { it.id == id } ?: return@forEach
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Avatar(contact, size = 52.dp)
                                Box(
                                    Modifier.align(Alignment.BottomEnd).size(23.dp).clip(CircleShape).background(SecondaryText)
                                        .clickable { selectedIds = selectedIds - id },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.Close, "Remove ${contact.name}", tint = AppBackground, modifier = Modifier.size(17.dp)) }
                            }
                            Text(contact.name, color = SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(72.dp))
                        }
                    }
                }
                Divider(color = DividerColor)
            }
            Text("Frequently contacted", color = SecondaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(contacts, key = { it.id }) { contact ->
                    val selected = contact.id in selectedIds
                    Row(
                        Modifier.fillMaxWidth().height(68.dp).clickable { selectedIds = if (selected) selectedIds - contact.id else selectedIds + contact.id }
                            .padding(start = 16.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(contact, size = 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(contact.name, color = PrimaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .border(1.5.dp, if (selected) ActionWhite else SecondaryText, CircleShape)
                                .background(if (selected) ActionWhite else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = AppBackground, modifier = Modifier.size(17.dp))
                        }
                    }
                    Divider(color = DividerColor, modifier = Modifier.padding(start = 70.dp))
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        } else Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { namingStep = false }) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
                Text("New group", color = PrimaryText, style = MaterialTheme.typography.titleLarge)
            }
            Divider(color = DividerColor)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(ComponentSurface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CameraAlt, "Add group icon", tint = SecondaryText, modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    modifier = Modifier.weight(1f).height(52.dp).border(1.5.dp, PrimaryText, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp).focusRequester(nameFocusRequester),
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = PrimaryText),
                    cursorBrush = SolidColor(BrandGreen),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (title.isEmpty()) Text("Group name (optional)", color = SecondaryText)
                            inner()
                        }
                    },
                )
                IconButton(onClick = {}) { Icon(Icons.Rounded.EmojiEmotions, "Emoji", tint = SecondaryText) }
            }
            GroupSetupRow(Icons.Rounded.AutoDelete, "Disappearing messages", "Off")
            GroupSetupRow(Icons.Rounded.Settings, "Group permissions", null)
            Text("Members: ${selectedIds.size}", color = SecondaryText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                selectedIds.forEach { id ->
                    state.contacts.firstOrNull { it.id == id }?.let { contact ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Avatar(contact, size = 52.dp)
                            Text(contact.name, color = SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(76.dp))
                        }
                    }
                }
            }
        }
        if (!namingStep && selectedIds.isNotEmpty()) {
            FloatingActionButton(
                onClick = { namingStep = true },
                containerColor = ActionWhite,
                contentColor = AppBackground,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { Icon(Icons.Rounded.Send, "Next") }
        } else if (namingStep) {
            FloatingActionButton(
                onClick = { onCreate(title.trim().ifBlank { "New group" }, selectedIds) },
                containerColor = ActionWhite,
                contentColor = AppBackground,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { Icon(Icons.Rounded.Check, "Create group") }
        }
    }
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = ElevatedSurface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Discard group?", color = PrimaryText, style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Your changes won't be saved if you leave before creating the group.", color = SecondaryText, style = MaterialTheme.typography.bodyLarge) },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel", color = BrandGreen) } },
            confirmButton = { TextButton(onClick = onBack) { Text("Discard group", color = BrandGreen) } },
        )
    }
}

@Composable
private fun GroupSetupRow(icon: ImageVector, title: String, subtitle: String?) {
    Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryText, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, color = SecondaryText, style = MaterialTheme.typography.bodyLarge) }
        }
        Icon(icon, null, tint = SecondaryText, modifier = Modifier.size(27.dp))
    }
}

@Composable
fun ForwardPickerScreen(
    state: AppState,
    sourceConversationId: String,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
            Text("Forward to…", color = PrimaryText, style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.chats.filter { it.id != sourceConversationId }, key = { it.id }) { chat ->
                ChatRow(
                    thread = chat,
                    onClick = { onPick(chat.id) },
                    onLongClick = {},
                    onAvatarClick = { onPick(chat.id) },
                )
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
    onSignOut: () -> Unit = {},
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
            SettingSwitchRow("Mute notifications", state.messageNotificationsMuted) {
                dispatch(AppAction.ToggleMessageMute)
            }
            SettingRow("Notification tone", "System default") { showMessage(V1_UNAVAILABLE_MESSAGE) }
            SettingRow("Vibrate", "System default") { showMessage(V1_UNAVAILABLE_MESSAGE) }
            SettingRow("Advanced settings", null) { showMessage(V1_UNAVAILABLE_MESSAGE) }
            Divider(color = DividerColor)
            SettingRow("Sign out", "Clears messages cached on this device") { onSignOut() }
        }
    }
}

private const val V1_UNAVAILABLE_MESSAGE = "Not available in this version"

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
