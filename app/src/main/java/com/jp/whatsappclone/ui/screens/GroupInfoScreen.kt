package com.jp.whatsappclone.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VoiceChat
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.data.ContactUi
import com.jp.whatsappclone.data.GroupMemberUi
import com.jp.whatsappclone.ui.ActionWhite
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.BrandGreen
import com.jp.whatsappclone.ui.ComponentSurface
import com.jp.whatsappclone.ui.DividerColor
import com.jp.whatsappclone.ui.PrimaryText
import com.jp.whatsappclone.ui.SecondaryText
import com.jp.whatsappclone.ui.components.Avatar
import com.jp.whatsappclone.ui.components.avatarDrawableFor

@Composable
fun GroupInfoScreen(
    threadId: String,
    state: AppState,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
) {
    val thread = state.chats.firstOrNull { it.id == threadId } ?: return
    val members = state.groupMembers.filter { it.groupId == thread.contact.id }
    val listState = rememberLazyListState()
    val compactHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 260 }
    }
    var chatLocked by rememberSaveable { mutableStateOf(false) }
    var translationEnabled by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBackground)) {
        GroupInfoTopBar(
            contact = thread.contact,
            compact = compactHeader,
            onBack = onBack,
            onAction = showMessage,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item {
                GroupProfileHeader(
                    contact = thread.contact,
                    memberCount = 58,
                    onAction = showMessage,
                )
            }
            item { GroupMediaSection(state, showMessage) }
            item {
                GroupInfoRow(Icons.Outlined.Storage, "Manage storage", "1.6 GB") { showMessage("Storage management is mocked") }
                GroupInfoRow(Icons.Outlined.Notifications, "Notifications", "All") { showMessage("Group notifications are mocked") }
                GroupInfoRow(Icons.Outlined.PhotoLibrary, "Media visibility", "Default (Yes)") { showMessage("Media visibility is mocked") }
                GroupInfoRow(Icons.Outlined.DarkMode, "Chat theme", "System default") { showMessage("Group theme is mocked") }
                GroupDivider()
            }
            item {
                GroupInfoRow(Icons.Outlined.EnhancedEncryption, "Encryption", "Messages and calls are end-to-end encrypted. Learn more") {
                    showMessage("Encryption information")
                }
                GroupInfoRow(Icons.Outlined.AutoDelete, "Disappearing messages", "Off") { showMessage("Disappearing messages are mocked") }
                GroupInfoRow(
                    icon = Icons.Outlined.Lock,
                    title = "Chat lock",
                    subtitle = "Lock and hide this chat on this device.",
                    switchValue = chatLocked,
                    onSwitch = { chatLocked = it },
                )
                GroupInfoRow(Icons.Outlined.Security, "Advanced chat privacy", "Off") { showMessage("Advanced privacy is mocked") }
                GroupInfoRow(Icons.Outlined.AdminPanelSettings, "Group permissions") { showMessage("Group permissions are mocked") }
                GroupInfoRow(
                    icon = Icons.Outlined.Language,
                    title = "Translate messages",
                    switchValue = translationEnabled,
                    onSwitch = { translationEnabled = it },
                )
                GroupInfoRow(
                    icon = Icons.Outlined.GroupAdd,
                    title = "Create a similar group",
                    subtitle = "Start with the same members that you can add or remove.",
                    lightIcon = true,
                ) { showMessage("Similar-group creation is mocked") }
            }
            item { MemberSectionHeader(58) { showMessage("Member search is mocked") } }
            item {
                MemberActionRow(Icons.Outlined.GroupAdd, "Add members") { showMessage("Member picker is mocked") }
                MemberActionRow(Icons.Outlined.Link, "Invite via link or QR code") { showMessage("Invite link is mocked") }
            }
            items(members, key = { it.contact.id }) { member ->
                GroupMemberRow(member) { showMessage("${member.contact.name} profile is mocked") }
            }
        }
    }
}

@Composable
private fun GroupInfoTopBar(
    contact: ContactUi,
    compact: Boolean,
    onBack: () -> Unit,
    onAction: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(AppBackground).statusBarsPadding().height(60.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = PrimaryText) }
        if (compact) {
            Avatar(contact, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Text(contact.name, color = PrimaryText, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        IconButton(onClick = { onAction("Add to lists is mocked") }) { Icon(Icons.Outlined.AddPhotoAlternate, "Add to lists", tint = PrimaryText) }
        IconButton(onClick = { onAction("Group QR is mocked") }) { Icon(Icons.Outlined.QrCode2, "Group QR", tint = PrimaryText) }
        IconButton(onClick = { onAction("More group options are mocked") }) { Icon(Icons.Rounded.MoreVert, "More", tint = PrimaryText) }
    }
}

@Composable
private fun GroupProfileHeader(contact: ContactUi, memberCount: Int, onAction: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(contact, size = 116.dp)
        Spacer(Modifier.height(22.dp))
        Text(contact.name, color = PrimaryText, fontSize = 27.sp, fontWeight = FontWeight.Normal)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Group · ", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
            Text("$memberCount members", color = BrandGreen, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            "Add group description",
            color = BrandGreen,
            fontSize = 18.sp,
            modifier = Modifier.clickable { onAction("Group description editor is mocked") }.padding(vertical = 15.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GroupActionButton(Icons.Outlined.VoiceChat, "Voice chat") { onAction("Mock group voice chat started") }
            GroupActionButton(Icons.Outlined.GroupAdd, "Add") { onAction("Member picker is mocked") }
            GroupActionButton(Icons.Outlined.Search, "Search") { onAction("Group search is mocked") }
        }
        GroupInfoRow(
            icon = Icons.Outlined.AddPhotoAlternate,
            title = "Add to lists",
            modifier = Modifier.padding(top = 18.dp),
        ) { onAction("Add to lists is mocked") }
    }
}

@Composable
private fun GroupActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.width(84.dp).height(54.dp).clip(RoundedCornerShape(28.dp)).background(ComponentSurface).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = PrimaryText, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GroupMediaSection(state: AppState, onAction: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { onAction("Media gallery is mocked") }.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Media, links, and docs", color = SecondaryText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("1,043", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Rounded.ChevronRight, null, tint = SecondaryText)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.contacts.filter { !it.isGroup }.take(5), key = { it.id }) { contact ->
                MediaPreviewTile(contact)
            }
        }
        Spacer(Modifier.height(18.dp))
        GroupDivider()
    }
}

@Composable
private fun MediaPreviewTile(contact: ContactUi) {
    val drawable = avatarDrawableFor(contact)
    Box(Modifier.size(width = 112.dp, height = 92.dp).clip(RoundedCornerShape(4.dp)).background(ComponentSurface)) {
        if (drawable != null) {
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color(contact.color)), contentAlignment = Alignment.Center) {
                Text(contact.initials, color = PrimaryText, fontSize = 24.sp)
            }
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0xA8000000)).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text(if (contact.id.hashCode() % 2 == 0) "GIF" else "0:12", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GroupInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    lightIcon: Boolean = false,
    switchValue: Boolean? = null,
    onSwitch: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).then(if (lightIcon) Modifier.clip(CircleShape).background(ActionWhite) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (lightIcon) AppBackground else SecondaryText, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryText, fontSize = 17.sp)
            subtitle?.let {
                Text(it, color = SecondaryText, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (switchValue != null && onSwitch != null) {
            Switch(
                checked = switchValue,
                onCheckedChange = onSwitch,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppBackground,
                    checkedTrackColor = BrandGreen,
                    uncheckedThumbColor = SecondaryText,
                    uncheckedTrackColor = ComponentSurface,
                    uncheckedBorderColor = SecondaryText,
                ),
            )
        }
    }
}

@Composable
private fun MemberSectionHeader(count: Int, onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 7.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$count members", color = SecondaryText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search members", tint = SecondaryText) }
    }
}

@Composable
private fun MemberActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(ActionWhite), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AppBackground, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, color = PrimaryText, fontSize = 17.sp)
    }
}

@Composable
private fun GroupMemberRow(member: GroupMemberUi, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(member.contact, size = 48.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(member.contact.name, color = PrimaryText, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (member.about.isNotBlank()) {
                Text(member.about, color = SecondaryText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (member.isAdmin) {
            Text(
                "Group Admin",
                color = PrimaryText,
                fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ComponentSurface).padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun GroupDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
}
