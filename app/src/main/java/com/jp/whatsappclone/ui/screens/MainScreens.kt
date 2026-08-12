package com.jp.whatsappclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jp.whatsappclone.data.AppAction
import com.jp.whatsappclone.data.AppState
import com.jp.whatsappclone.data.CallDirection
import com.jp.whatsappclone.data.CallLogUi
import com.jp.whatsappclone.data.ChannelUi
import com.jp.whatsappclone.data.ChatThreadUi
import com.jp.whatsappclone.data.ContactUi
import com.jp.whatsappclone.data.MainTab
import com.jp.whatsappclone.data.StatusUi
import com.jp.whatsappclone.data.ToolItemUi
import com.jp.whatsappclone.data.filteredHomeChats
import com.jp.whatsappclone.ui.ActionWhite
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.BrandGreen
import com.jp.whatsappclone.ui.ComponentSurface
import com.jp.whatsappclone.ui.DividerColor
import com.jp.whatsappclone.ui.ElevatedSurface
import com.jp.whatsappclone.ui.PrimaryText
import com.jp.whatsappclone.ui.SecondaryText
import com.jp.whatsappclone.ui.components.AppTopBar
import com.jp.whatsappclone.ui.components.ArchivedRow
import com.jp.whatsappclone.ui.components.Avatar
import com.jp.whatsappclone.ui.components.ChatRow
import com.jp.whatsappclone.ui.components.DarkFloatingButton
import com.jp.whatsappclone.ui.components.FilterChipRow
import com.jp.whatsappclone.ui.components.LightFloatingButton
import com.jp.whatsappclone.ui.components.WhatsAppBottomNavigation
import com.jp.whatsappclone.ui.components.WhatsAppSearchBar
import com.jp.whatsappclone.ui.components.avatarDrawableFor

@Composable
fun MainScreen(
    state: AppState,
    dispatch: (AppAction) -> Unit,
    onOpenChat: (String) -> Unit,
    onSearch: () -> Unit,
    onContactPicker: () -> Unit,
    onNotifications: () -> Unit,
    showMessage: (String) -> Unit,
    initialPreviewContactId: String? = null,
    initialOverflowMenu: Boolean = false,
    toolsStartIndex: Int = 0,
) {
    var previewContact by remember(initialPreviewContactId) {
        mutableStateOf(state.contacts.firstOrNull { it.id == initialPreviewContactId })
    }
    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            WhatsAppBottomNavigation(
                selected = state.selectedTab,
                unreadCount = state.chats.sumOf { it.unreadCount },
                onSelect = { dispatch(AppAction.SelectTab(it)) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.selectedTab) {
                MainTab.Chats -> ChatsScreen(
                    state = state,
                    onSearch = onSearch,
                    onChat = onOpenChat,
                    onAvatar = { previewContact = it },
                    onContactPicker = onContactPicker,
                    onNotifications = onNotifications,
                    onFilter = { dispatch(AppAction.ToggleSearchFilter(it)) },
                    showMessage = showMessage,
                    initialOverflowMenu = initialOverflowMenu,
                )
                MainTab.Calls -> CallsScreen(
                    state = state,
                    onCall = { contact, video ->
                        dispatch(AppAction.StartCall(contact.id, video))
                        showMessage(if (video) "Mock video call started" else "Mock call started")
                    },
                    showMessage = showMessage,
                )
                MainTab.Updates -> UpdatesScreen(state, showMessage)
                MainTab.Tools -> ToolsScreen(
                    state = state,
                    onDismissAd = { dispatch(AppAction.DismissDraftAd) },
                    showMessage = showMessage,
                    startIndex = toolsStartIndex,
                )
            }
        }
    }
    previewContact?.let { contact ->
        ContactPreviewDialog(
            contact = contact,
            onDismiss = { previewContact = null },
            onMessage = {
                previewContact = null
                state.chats.firstOrNull { it.contact.id == contact.id }?.let { onOpenChat(it.id) }
                    ?: showMessage("New mock conversation")
            },
            onAction = showMessage,
        )
    }
}

@Composable
private fun ChatsScreen(
    state: AppState,
    onSearch: () -> Unit,
    onChat: (String) -> Unit,
    onAvatar: (ContactUi) -> Unit,
    onContactPicker: () -> Unit,
    onNotifications: () -> Unit,
    onFilter: (String) -> Unit,
    showMessage: (String) -> Unit,
    initialOverflowMenu: Boolean,
) {
    var overflow by remember(initialOverflowMenu) { mutableStateOf(initialOverflowMenu) }
    var filtersVisible by remember(initialOverflowMenu) { mutableStateOf(initialOverflowMenu) }
    var selectedChatId by rememberSaveable { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Column(Modifier.fillMaxSize()) {
            Box {
                if (selectedChatId != null) {
                    ChatSelectionTopBar(
                        onBack = { selectedChatId = null },
                        onAction = { action ->
                            showMessage("$action is a mock chat action")
                            if (action != "More") selectedChatId = null
                        },
                    )
                } else {
                    AppTopBar(
                        title = "WhatsApp",
                        brand = true,
                        onCamera = { showMessage("Camera is mocked in this UI prototype") },
                        onMore = {
                            filtersVisible = true
                            overflow = true
                        },
                    )
                }
                Box(
                    Modifier.align(Alignment.TopEnd).statusBarsPadding().width(44.dp).height(56.dp),
                ) {
                    DropdownMenu(
                        expanded = overflow && selectedChatId == null,
                        onDismissRequest = { overflow = false },
                        modifier = Modifier.width(184.dp).background(ElevatedSurface),
                    ) {
                    listOf("Manage Ads", "New group", "Business broadcasts", "Communities", "Lists", "Linked devices", "Starred").forEach { label ->
                        DropdownMenuItem(
                            text = { Text(label, color = PrimaryText, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                overflow = false
                                showMessage("$label is a mock action")
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(BrandGreen))
                                Spacer(Modifier.width(10.dp))
                                Text("Settings", color = PrimaryText, style = MaterialTheme.typography.bodyLarge)
                            }
                        },
                        onClick = {
                            overflow = false
                            onNotifications()
                        },
                    )
                    }
                }
            }
            WhatsAppSearchBar(onClick = onSearch, modifier = Modifier.padding(horizontal = 12.dp))
            if (filtersVisible) {
                Spacer(Modifier.height(28.dp))
                FilterChipRow(
                    filters = listOf("Hi ma", "Bang meet", "Work", "Unread"),
                    selected = state.searchFilters,
                    onFilter = onFilter,
                    modifier = Modifier.padding(start = 12.dp),
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Spacer(Modifier.height(20.dp))
            }
            LazyColumn(Modifier.fillMaxSize()) {
                item { ArchivedRow(count = 28) }
                items(state.filteredHomeChats(), key = { it.id }) { thread ->
                    ChatRow(
                        thread = thread,
                        selected = selectedChatId == thread.id,
                        onClick = {
                            if (selectedChatId != null) selectedChatId = if (selectedChatId == thread.id) null else thread.id
                            else onChat(thread.id)
                        },
                        onLongClick = {
                            selectedChatId = thread.id
                            filtersVisible = true
                        },
                        onAvatarClick = {
                            if (selectedChatId != null) selectedChatId = thread.id else onAvatar(thread.contact)
                        },
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
        LightFloatingButton(
            icon = Icons.Rounded.BusinessCenter,
            description = "New chat",
            onClick = onContactPicker,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            showAddMark = true,
        )
    }
}

@Composable
private fun ChatSelectionTopBar(onBack: () -> Unit, onAction: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(ElevatedSurface).statusBarsPadding().height(60.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Close selection", tint = PrimaryText) }
        Text("1", color = PrimaryText, fontSize = 22.sp)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { onAction("Archive") }) { Icon(Icons.Rounded.Archive, "Archive", tint = PrimaryText) }
        IconButton(onClick = { onAction("Pin") }) { Icon(Icons.Rounded.PushPin, "Pin", tint = PrimaryText) }
        IconButton(onClick = { onAction("Delete") }) { Icon(Icons.Rounded.Delete, "Delete", tint = PrimaryText) }
        IconButton(onClick = { onAction("Mute") }) { Icon(Icons.Rounded.NotificationsOff, "Mute", tint = PrimaryText) }
        IconButton(onClick = { onAction("More") }) { Icon(Icons.Rounded.MoreVert, "More", tint = PrimaryText) }
    }
}

@Composable
private fun CallsScreen(
    state: AppState,
    onCall: (ContactUi, Boolean) -> Unit,
    showMessage: (String) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                "Calls",
                onCamera = { showMessage("Camera is mocked") },
                onSearch = { showMessage("Call search is mocked") },
                onMore = { showMessage("Call settings are mocked") },
            )
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item { CallShortcut(Icons.Outlined.Call, "Call") { showMessage("Choose a contact to call") } }
                item { CallShortcut(Icons.Rounded.CalendarMonth, "Schedule") { showMessage("Call scheduled") } }
                item { CallShortcut(Icons.Rounded.Dialpad, "Keypad") { showMessage("Keypad is mocked") } }
                items(state.contacts.take(3)) { contact ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(contact, modifier = Modifier.clickable { onCall(contact, false) }, size = 56.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(contact.name.substringBefore(' '), color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item { CallShortcut(Icons.Rounded.FavoriteBorder, "Favourites") { showMessage("Favourites selected") } }
            }
            Text(
                "Recent",
                color = PrimaryText,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.calls, key = { it.id }) { call -> CallRow(call, onCall) }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
        LightFloatingButton(
            Icons.Rounded.Phone,
            "Add call",
            { showMessage("Choose a contact for a new call") },
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
            showAddMark = true,
            addMarkColor = AppBackground,
            addMarkOffsetX = 9.dp,
            addMarkOffsetY = (-9).dp,
        )
    }
}

@Composable
private fun CallShortcut(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(ComponentSurface).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = PrimaryText, modifier = Modifier.size(25.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(label, color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CallRow(call: CallLogUi, onCall: (ContactUi, Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).clickable { onCall(call.contact, call.video) }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(call.contact, size = 38.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                call.contact.name,
                color = if (call.direction == CallDirection.Missed) Color(0xFFF06475) else PrimaryText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (call.direction == CallDirection.Outgoing) "↗" else "↙",
                    color = if (call.direction == CallDirection.Missed) Color(0xFFF06475) else BrandGreen,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = call.whenLabel,
                    color = if (call.direction == CallDirection.Missed) Color(0xFFF06475) else SecondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Icon(if (call.video) Icons.Outlined.VideoCall else Icons.Outlined.Call, null, tint = PrimaryText)
    }
}

@Composable
private fun UpdatesScreen(state: AppState, showMessage: (String) -> Unit) {
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                AppTopBar(
                    "Updates",
                    onCamera = { showMessage("Status camera is mocked") },
                    onSearch = { showMessage("Update search is mocked") },
                    onMore = { showMessage("Update options are mocked") },
                )
            }
            item {
                Text("Status", color = PrimaryText, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 8.dp))
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.statuses, key = { it.id }) { status -> StatusCard(status) { showMessage("${status.contact.name} status viewer is mocked") } }
                }
                Box(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth().height(38.dp).clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF485158), RoundedCornerShape(24.dp)).clickable { showMessage("Status boost is mocked") },
                contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Campaign, null, tint = BrandGreen, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Boost status", color = BrandGreen, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Channels", color = PrimaryText, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(22.dp)).background(ComponentSurface).clickable { showMessage("Explore channels") }.padding(horizontal = 18.dp, vertical = 7.dp)) {
                        Text("Explore", color = PrimaryText, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            items(state.channels, key = { it.id }) { channel -> ChannelRow(channel) { showMessage("${channel.name} is a mock channel") } }
            item { Spacer(Modifier.height(96.dp)) }
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DarkFloatingButton(Icons.Rounded.Edit, "Create text status", { showMessage("Text status composer is mocked") })
            LightFloatingButton(
                Icons.Rounded.CameraAlt,
                "Create status",
                { showMessage("Status camera is mocked") },
                showAddMark = true,
                addMarkColor = AppBackground,
                addMarkOffsetX = 9.dp,
                addMarkOffsetY = (-9).dp,
            )
        }
    }
}

@Composable
private fun StatusCard(status: StatusUi, onClick: () -> Unit) {
    Box(
        Modifier.width(78.dp).height(140.dp).clip(RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(listOf(Color(status.gradientStart), Color(status.gradientEnd))))
            .clickable(onClick = onClick),
    ) {
        avatarDrawableFor(status.contact)?.let { drawable ->
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        .58f to Color.Transparent,
                        1f to Color(0xD9000000),
                    ),
                ),
            )
        }
        Avatar(status.contact, modifier = Modifier.padding(8.dp).border(2.dp, BrandGreen, CircleShape), size = 38.dp)
        if (status.mine) {
            Box(Modifier.align(Alignment.TopStart).padding(start = 36.dp, top = 33.dp).size(22.dp).clip(CircleShape).background(ActionWhite), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, tint = AppBackground, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            status.caption,
            color = ActionWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChannelRow(channel: ChannelUi, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(channel.color)), contentAlignment = Alignment.Center) {
            Text(channel.name.take(2).uppercase(), color = PrimaryText, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = PrimaryText, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(channel.preview, color = SecondaryText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(channel.time, color = BrandGreen, style = MaterialTheme.typography.bodyMedium)
            Badge(containerColor = BrandGreen, contentColor = AppBackground) { Text(channel.unreadCount.toString()) }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ToolsScreen(
    state: AppState,
    onDismissAd: () -> Unit,
    showMessage: (String) -> Unit,
    startIndex: Int,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    LazyColumn(Modifier.fillMaxSize().background(AppBackground), state = listState) {
        stickyHeader { AppTopBar("Tools", onCamera = { showMessage("Camera is mocked") }, onMore = { showMessage("Tool options are mocked") }) }
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Last 7 days performance", color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Rounded.Info, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
            }
            LazyRow(
                modifier = Modifier.padding(top = 12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.metrics) { metric ->
                    Card(
                        Modifier.width(105.dp).height(102.dp).clickable { showMessage("${metric.label.replace("\n", " ")} details") },
                        colors = CardDefaults.cardColors(containerColor = AppBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30383D)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Icon(if (metric.icon == "catalogue") Icons.Rounded.GridOn else Icons.Rounded.Chat, null, tint = PrimaryText)
                            Spacer(Modifier.height(8.dp))
                            Text(metric.value + if (metric.positive) " ↗" else "", color = if (metric.positive) BrandGreen else PrimaryText, style = MaterialTheme.typography.titleMedium)
                            Text(metric.label, color = PrimaryText, style = MaterialTheme.typography.bodyMedium, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
        if (state.draftAdVisible) {
            item {
                Text("For you", color = PrimaryText, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
                Card(
                    Modifier.padding(start = 12.dp).width(315.dp).height(130.dp),
                    colors = CardDefaults.cardColors(containerColor = AppBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30383D)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(Modifier.padding(14.dp)) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF38212B)), contentAlignment = Alignment.Center) { Text("AD", color = BrandGreen) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Draft Ad", color = PrimaryText, style = MaterialTheme.typography.titleMedium)
                            Text("We’ve saved your ad progress so you can finish creating it.", color = SecondaryText, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.clip(RoundedCornerShape(24.dp)).background(ActionWhite).clickable { showMessage("Ad creation is mocked") }.padding(horizontal = 18.dp, vertical = 9.dp)) {
                                Text("Continue creating ad", color = AppBackground, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        IconButton(onClick = onDismissAd) { Icon(Icons.Rounded.Close, "Dismiss ad", tint = SecondaryText) }
                    }
                }
            }
        }
        item { ToolSectionTitle("Grow your business") }
        itemsIndexed(state.toolItems) { index, item ->
            when (index) {
                4 -> ToolSectionTitle("Reach more customers")
                6 -> ToolSectionTitle("Organise your chats")
                10 -> ToolSectionTitle("Manage your account")
            }
            ToolRow(item) { showMessage("${item.title} is a mock business tool") }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ToolSectionTitle(title: String) {
    Text(title, color = PrimaryText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp))
}

@Composable
private fun ToolRow(item: ToolItemUi, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(74.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                when (item.icon) {
                    "verified" -> Icons.Rounded.Verified
                    "catalogue" -> Icons.Rounded.GridOn
                    "profile" -> Icons.Rounded.Storefront
                    else -> Icons.Rounded.TrendingUp
                },
                null,
                tint = if (item.accent) BrandGreen else SecondaryText,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = PrimaryText, style = MaterialTheme.typography.titleMedium)
            Text(item.subtitle, color = SecondaryText, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (item.accent) Box(Modifier.size(10.dp).clip(CircleShape).background(BrandGreen))
    }
}

@Composable
private fun ContactPreviewDialog(
    contact: ContactUi,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onAction: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.width(240.dp).clip(RoundedCornerShape(2.dp)).background(ElevatedSurface),
        ) {
            Box(Modifier.fillMaxWidth().height(240.dp).background(Color(contact.color))) {
                if (contact.isGroup) Icon(Icons.Rounded.Storefront, null, tint = BrandGreen, modifier = Modifier.size(92.dp).align(Alignment.Center))
                else avatarDrawableFor(contact)?.let { drawable ->
                    Image(
                        painter = painterResource(drawable),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } ?: Text(contact.initials, color = PrimaryText, fontSize = 72.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Center))
                Text(contact.name, color = PrimaryText, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopStart).padding(10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth().height(46.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMessage) { Icon(Icons.Rounded.Chat, "Message", tint = PrimaryText) }
                IconButton(onClick = { onAction("Mock voice call") }) { Icon(Icons.Rounded.Phone, "Voice call", tint = PrimaryText) }
                IconButton(onClick = { onAction("Mock video call") }) { Icon(Icons.Rounded.VideoCall, "Video call", tint = PrimaryText) }
                IconButton(onClick = { onAction("Contact info is mocked") }) { Icon(Icons.Rounded.Info, "Info", tint = PrimaryText) }
            }
        }
    }
}
