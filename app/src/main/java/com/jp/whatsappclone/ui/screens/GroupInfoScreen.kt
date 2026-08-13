package com.jp.whatsappclone.ui.screens

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@Composable
fun GroupInfoScreen(
    threadId: String,
    state: AppState,
    dispatch: (com.jp.whatsappclone.data.AppAction) -> Unit,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
) {
    val thread = state.chats.firstOrNull { it.id == threadId } ?: return
    val members = state.groupMembers.filter { it.groupId == thread.id }
    val listState = rememberLazyListState()
    val compactHeader by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 260 }
    }
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
                    memberCount = members.size,
                    onAction = showMessage,
                )
            }
            item { GroupMediaSection(showMessage) }
            item {
                GroupInfoRow(Icons.Outlined.Storage, "Manage storage", "Messages and media") { showMessage(V1_UNAVAILABLE_MESSAGE) }
                GroupInfoRow(
                    Icons.Outlined.Notifications,
                    "Notifications",
                    if (thread.muted) "Muted" else "All",
                ) {
                    dispatch(com.jp.whatsappclone.data.AppAction.SetConversationMuted(thread.id, !thread.muted))
                }
                GroupInfoRow(Icons.Outlined.PhotoLibrary, "Media visibility", "Default") { showMessage(V1_UNAVAILABLE_MESSAGE) }
                GroupInfoRow(Icons.Outlined.DarkMode, "Chat theme", "System default") { showMessage(V1_UNAVAILABLE_MESSAGE) }
                GroupDivider()
            }
            item {
                GroupInfoRow(Icons.Outlined.Security, "Security", "Firebase Auth and Firestore rules protect access") {
                    showMessage("Firebase transport and Storage security protect this chat")
                }
                GroupInfoRow(Icons.Outlined.AutoDelete, "Disappearing messages", "Off") { showMessage(V1_UNAVAILABLE_MESSAGE) }
                GroupInfoRow(
                    icon = Icons.Outlined.Lock,
                    title = "Chat lock",
                    subtitle = "Off",
                    switchValue = false,
                    onSwitch = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                    onClick = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                )
                GroupInfoRow(Icons.Outlined.Security, "Advanced chat privacy", "Off") { showMessage(V1_UNAVAILABLE_MESSAGE) }
                GroupInfoRow(Icons.Outlined.AdminPanelSettings, "Group permissions") { showMessage(V1_UNAVAILABLE_MESSAGE) }
                GroupInfoRow(
                    icon = Icons.Outlined.Language,
                    title = "Translate messages",
                    switchValue = false,
                    onSwitch = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                    onClick = { showMessage(V1_UNAVAILABLE_MESSAGE) },
                )
                GroupInfoRow(
                    icon = Icons.Outlined.GroupAdd,
                    title = "Create a similar group",
                    subtitle = "Start with the same members that you can add or remove.",
                    lightIcon = true,
                ) { showMessage(V1_UNAVAILABLE_MESSAGE) }
            }
            item { MemberSectionHeader(members.size) { showMessage(V1_UNAVAILABLE_MESSAGE) } }
            item {
                MemberActionRow(Icons.Outlined.GroupAdd, "Add members") { showMessage(V1_UNAVAILABLE_MESSAGE) }
                MemberActionRow(Icons.Outlined.Link, "Invite via link or QR code") { showMessage(V1_UNAVAILABLE_MESSAGE) }
            }
            items(members, key = { it.contact.id }) { member ->
                GroupMemberRow(member) { showMessage(V1_UNAVAILABLE_MESSAGE) }
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
        IconButton(onClick = { onAction(V1_UNAVAILABLE_MESSAGE) }) { Icon(Icons.Outlined.AddPhotoAlternate, "Add to lists", tint = PrimaryText) }
        IconButton(onClick = { onAction(V1_UNAVAILABLE_MESSAGE) }) { Icon(Icons.Outlined.QrCode2, "Group QR", tint = PrimaryText) }
        IconButton(onClick = { onAction(V1_UNAVAILABLE_MESSAGE) }) { Icon(Icons.Rounded.MoreVert, "More", tint = PrimaryText) }
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
            modifier = Modifier.clickable { onAction(V1_UNAVAILABLE_MESSAGE) }.padding(vertical = 15.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GroupActionButton(Icons.Outlined.VoiceChat, "Voice chat") { onAction(V1_UNAVAILABLE_MESSAGE) }
            GroupActionButton(Icons.Outlined.GroupAdd, "Add") { onAction(V1_UNAVAILABLE_MESSAGE) }
            GroupActionButton(Icons.Outlined.Search, "Search") { onAction(V1_UNAVAILABLE_MESSAGE) }
        }
        GroupInfoRow(
            icon = Icons.Outlined.AddPhotoAlternate,
            title = "Add to lists",
            modifier = Modifier.padding(top = 18.dp),
        ) { onAction(V1_UNAVAILABLE_MESSAGE) }
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
private fun GroupMediaSection(onAction: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { onAction(V1_UNAVAILABLE_MESSAGE) }.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Media, links, and docs", color = SecondaryText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Not enabled", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Rounded.ChevronRight, null, tint = SecondaryText)
        }
        GroupDivider()
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

private const val V1_UNAVAILABLE_MESSAGE = "Not available in this version"
