package com.jp.whatsappclone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jp.whatsappclone.data.ChatThreadUi
import com.jp.whatsappclone.data.ContactUi
import com.jp.whatsappclone.data.DeliveryStatus
import com.jp.whatsappclone.data.MainTab
import com.jp.whatsappclone.ui.ActionWhite
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.BrandGreen
import com.jp.whatsappclone.ui.ComponentSurface
import com.jp.whatsappclone.ui.DividerColor
import com.jp.whatsappclone.ui.PrimaryText
import com.jp.whatsappclone.ui.ReadBlue
import com.jp.whatsappclone.ui.SearchSurface
import com.jp.whatsappclone.ui.SecondaryText
import com.jp.whatsappclone.ui.SelectedPill
import com.jp.whatsappclone.R
import kotlin.math.sin

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    brand: Boolean = false,
    onCamera: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppBackground)
            .statusBarsPadding()
            .height(60.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = if (brand) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineLarge,
            color = PrimaryText,
            maxLines = 1,
            modifier = Modifier,
        )
        Spacer(Modifier.weight(1f))
        onCamera?.let {
            IconButton(onClick = it, modifier = Modifier.size(40.dp).offset(y = (-1).dp)) {
                Icon(Icons.Outlined.CameraAlt, "Camera", tint = PrimaryText)
            }
        }
        onSearch?.let {
            IconButton(onClick = it, modifier = Modifier.size(40.dp).offset(y = (-1).dp)) {
                Icon(Icons.Rounded.Search, "Search", tint = PrimaryText)
            }
        }
        onMore?.let {
            IconButton(onClick = it, modifier = Modifier.size(40.dp).offset(y = (-1).dp)) {
                Icon(Icons.Rounded.MoreVert, "More options", tint = PrimaryText)
            }
        }
    }
}

@Composable
fun avatarDrawableFor(contact: ContactUi): Int? = when (contact.id) {
        "own" -> R.drawable.avatar_own
        "mia" -> R.drawable.avatar_mia
        "dev" -> R.drawable.avatar_dev
        "sara" -> R.drawable.avatar_sara
        "mona" -> R.drawable.avatar_mona
        "rohan" -> R.drawable.avatar_rohan
        "reena" -> R.drawable.avatar_reena
        "aisha" -> R.drawable.avatar_aisha
        else -> null
    }

@Composable
fun Avatar(
    contact: ContactUi,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val avatarResource = avatarDrawableFor(contact)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(contact.color))
            .semantics { contentDescription = "${contact.name} avatar" },
        contentAlignment = Alignment.Center,
    ) {
        if (contact.id == "team") {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "I",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * .38f).sp,
                )
                if (size >= 72.dp) {
                    Text(
                        "INNOVFIX",
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * .075f).sp,
                    )
                }
            }
        } else if (contact.isGroup) {
            Icon(Icons.Rounded.Groups, null, tint = BrandGreen, modifier = Modifier.size(size * .54f))
        } else if (avatarResource != null) {
            Image(
                painter = painterResource(avatarResource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = contact.initials,
                color = PrimaryText,
                fontWeight = FontWeight.Medium,
                fontSize = (size.value * .32f).sp,
            )
        }
    }
}

@Composable
fun WhatsAppSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    placeholder: String = "Search…",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(SearchSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = SecondaryText, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.ifBlank { placeholder },
            color = if (text.isBlank()) SecondaryText else PrimaryText,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun FilterChipRow(
    filters: List<String>,
    selected: Set<String>,
    onFilter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters) { filter ->
            val isSelected = filter in selected
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Color(0xFF153E2C) else AppBackground)
                    .border(1.dp, if (isSelected) BrandGreen else Color(0xFF30383D), RoundedCornerShape(14.dp))
                    .clickable { onFilter(filter) }
                    .padding(horizontal = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(filter, color = if (isSelected) BrandGreen else SecondaryText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ArchivedRow(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .offset(y = 2.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Archive, null, tint = SecondaryText)
        }
        Spacer(Modifier.width(12.dp))
        Text("Archived", color = SecondaryText, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text(count.toString(), color = SecondaryText, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ChatRow(
    thread: ChatThreadUi,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(if (selected) SelectedPill else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(
                thread.contact,
                modifier = Modifier.combinedClickable(
                    onClick = onAvatarClick,
                    onLongClick = onLongClick,
                ),
            )
            if (selected) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(ActionWhite),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, "Selected", tint = AppBackground, modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    thread.contact.name,
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    thread.time,
                    color = if (thread.unreadCount > 0) BrandGreen else SecondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    thread.draft -> Text("Draft: ", color = BrandGreen, style = MaterialTheme.typography.bodyMedium)
                    thread.mediaLabel != null -> Icon(Icons.Rounded.Image, null, tint = SecondaryText, modifier = Modifier.size(17.dp))
                    thread.preview.startsWith("You") -> Icon(Icons.Rounded.DoneAll, null, tint = ReadBlue, modifier = Modifier.size(18.dp))
                }
                if (thread.mediaLabel != null) Spacer(Modifier.width(4.dp))
                Text(
                    thread.preview.removePrefix("Draft: "),
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    fontStyle = if (thread.preview.contains("deleted", ignoreCase = true)) FontStyle.Italic else FontStyle.Normal,
                )
                if (thread.pinned) Icon(Icons.Rounded.PushPin, null, tint = SecondaryText, modifier = Modifier.size(17.dp))
                if (thread.muted) Icon(Icons.Rounded.NotificationsOff, null, tint = SecondaryText, modifier = Modifier.size(17.dp))
                if (thread.unreadCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Badge(containerColor = BrandGreen, contentColor = AppBackground) {
                        Text(thread.unreadCount.toString(), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppBottomNavigation(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    unreadCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppBackground)
            .border(0.5.dp, DividerColor, RoundedCornerShape(0.dp))
            .navigationBarsPadding()
            .height(80.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavigationItem(MainTab.Chats, Icons.Rounded.Chat, selected, unreadCount, onSelect)
        NavigationItem(MainTab.Calls, Icons.Rounded.Phone, selected, 0, onSelect)
        NavigationItem(MainTab.Updates, Icons.Rounded.Update, selected, 0, onSelect)
        NavigationItem(MainTab.Tools, Icons.Rounded.Storefront, selected, 0, onSelect)
    }
}

@Composable
private fun RowScope.NavigationItem(
    tab: MainTab,
    icon: ImageVector,
    selected: MainTab,
    badge: Int,
    onSelect: (MainTab) -> Unit,
) {
    val active = tab == selected
    val pillColor by animateColorAsState(if (active) SelectedPill else Color.Transparent, label = "navPill")
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .testTag("tab_${tab.name.lowercase()}")
            .clickable(interactionSource = interactionSource, indication = null) { onSelect(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, tab.name, tint = PrimaryText, modifier = Modifier.size(24.dp))
            if (badge > 0 && tab == MainTab.Chats) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = BrandGreen,
                    contentColor = AppBackground,
                ) { Text("99+", fontSize = 9.sp) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            tab.name,
            color = PrimaryText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun LightFloatingButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showAddMark: Boolean = false,
    addMarkColor: Color = ActionWhite,
    addMarkOffsetX: Dp = 0.dp,
    addMarkOffsetY: Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(ActionWhite)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = AppBackground, modifier = Modifier.size(27.dp))
        if (showAddMark) {
            Canvas(Modifier.size(8.dp).offset(addMarkOffsetX, addMarkOffsetY)) {
                val stroke = 1.7.dp.toPx()
                drawLine(addMarkColor, Offset(this.size.width / 2f, 0f), Offset(this.size.width / 2f, this.size.height), stroke, StrokeCap.Round)
                drawLine(addMarkColor, Offset(0f, this.size.height / 2f), Offset(this.size.width, this.size.height / 2f), stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun DarkFloatingButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComponentSurface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = PrimaryText, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun DoodleBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize().background(Color(0xFF0D1518))) {
        val color = Color(0xFF202D31)
        val stroke = .75.dp.toPx()
        val stepX = size.width / 12f
        val stepY = size.height / 24f
        for (row in 0..24) {
            for (col in 0..12) {
                val x = col * stepX + if (row % 2 == 0) 4.dp.toPx() else stepX / 2f
                val y = row * stepY + 4.dp.toPx()
                val radius = (4 + ((row + col) % 4) * 1.5f).dp.toPx()
                when ((row * 3 + col) % 4) {
                    0 -> {
                        drawCircle(color, radius, Offset(x, y), style = Stroke(stroke))
                        drawLine(
                            color,
                            Offset(x - 6.dp.toPx(), y + 7.dp.toPx()),
                            Offset(x + 7.dp.toPx(), y + 11.dp.toPx()),
                            stroke,
                            StrokeCap.Round,
                        )
                    }
                    1 -> drawRect(
                        color,
                        Offset(x - 6.dp.toPx(), y - 5.dp.toPx()),
                        androidx.compose.ui.geometry.Size(12.dp.toPx(), 10.dp.toPx()),
                        style = Stroke(stroke),
                    )
                    2 -> {
                        drawCircle(color, radius * .65f, Offset(x - 3.dp.toPx(), y), style = Stroke(stroke))
                        drawCircle(color, radius * .65f, Offset(x + 4.dp.toPx(), y), style = Stroke(stroke))
                    }
                    else -> {
                        val shape = Path().apply {
                            moveTo(x, y - 7.dp.toPx())
                            lineTo(x + 6.dp.toPx(), y + 5.dp.toPx())
                            lineTo(x - 6.dp.toPx(), y + 5.dp.toPx())
                            close()
                        }
                        drawPath(shape, color, style = Stroke(stroke))
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceWaveform(
    progress: Float,
    modifier: Modifier = Modifier,
    playedColor: Color = ReadBlue,
    remainingColor: Color = SecondaryText,
) {
    Canvas(modifier = modifier) {
        val bars = 38
        val gap = size.width / bars
        repeat(bars) { index ->
            val x = gap * index + gap / 2
            val fraction = index / bars.toFloat()
            val amplitude = .25f + (sin(index * 1.7).toFloat() + 1f) * .28f
            val height = size.height * amplitude
            drawLine(
                color = if (fraction <= progress) playedColor else remainingColor,
                start = Offset(x, (size.height - height) / 2),
                end = Offset(x, (size.height + height) / 2),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun DeliveryIcon(status: DeliveryStatus, modifier: Modifier = Modifier) {
    if (status == DeliveryStatus.None) return
    Icon(
        imageVector = if (status == DeliveryStatus.Sent) Icons.Rounded.Check else Icons.Rounded.DoneAll,
        contentDescription = status.name,
        tint = if (status == DeliveryStatus.Read) ReadBlue else SecondaryText,
        modifier = modifier.size(17.dp),
    )
}
