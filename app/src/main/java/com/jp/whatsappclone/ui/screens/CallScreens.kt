package com.jp.whatsappclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.outlined.SpeakerPhone
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jp.whatsappclone.data.ContactUi
import com.jp.whatsappclone.ui.AppBackground
import com.jp.whatsappclone.ui.ComponentSurface
import com.jp.whatsappclone.ui.PrimaryText
import com.jp.whatsappclone.ui.SecondaryText
import com.jp.whatsappclone.ui.components.DoodleBackground
import kotlinx.coroutines.delay

@Composable
fun ActiveCallScreen(
    contact: ContactUi,
    onEnd: () -> Unit,
    showMessage: (String) -> Unit,
) {
    var speaker by rememberSaveable { mutableStateOf(false) }
    var muted by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        DoodleBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically) {
                CallRoundButton(Icons.Outlined.CloseFullscreen, "Minimise", size = 52.dp) { showMessage("Call minimised") }
                Column(Modifier.weight(1f).padding(horizontal = 18.dp)) {
                    Text(contact.name, color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Call preview · not available in messaging v1", color = SecondaryText, fontSize = 16.sp)
                    }
                }
                CallRoundButton(Icons.Outlined.PersonAdd, "Add participant", size = 52.dp) { showMessage("Not available in messaging v1") }
            }
            Spacer(Modifier.height(124.dp))
            InitialsAvatar(contact, size = 192.dp)
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(Color(0xFF11171B)).padding(horizontal = 22.dp, vertical = 24.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CallControl(Icons.Outlined.SpeakerPhone, "Speaker", speaker) { speaker = !speaker }
                    CallControl(Icons.Outlined.Videocam, "Video", disabled = true) { }
                    CallControl(Icons.Outlined.MicOff, "Mute", muted) { muted = !muted }
                }
                Spacer(Modifier.height(30.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CallControl(Icons.Outlined.MoreHoriz, "More") { showMessage("Not available in messaging v1") }
                    CallControl(Icons.Outlined.ScreenShare, "Share", disabled = true) { showMessage("Not available in messaging v1") }
                    CallControl(Icons.Rounded.CallEnd, "End", destructive = true, onClick = onEnd)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun NoAnswerCallScreen(
    contact: ContactUi,
    onCancel: () -> Unit,
    onCallAgain: () -> Unit,
    showMessage: (String) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Box(
            Modifier.fillMaxSize().blur(28.dp).background(
                Brush.linearGradient(
                    listOf(Color(0xFF88827C), Color(0xFF2A2523), Color(0xFFB5B5B2), Color(0xFF080A0B)),
                ),
            ),
        )
        Box(Modifier.fillMaxSize().background(Color(0x4C000000)))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(75.dp))
            Text(contact.name, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("No answer", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(28.dp))
            InitialsAvatar(contact, size = 92.dp)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                ResultAction(Icons.Rounded.Close, "Cancel", Color.White, Color.Black, onCancel)
                ResultAction(Icons.Outlined.CameraAlt, "Record video note", ComponentSurface, Color.White) { showMessage("Not available in messaging v1") }
                ResultAction(Icons.Outlined.Videocam, "Call again", Color(0xFF19B765), Color.White, onCallAgain)
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun InitialsAvatar(contact: ContactUi, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF282552)), contentAlignment = Alignment.Center) {
        Text(contact.initials.take(2), color = Color(0xFFA68BFF), fontSize = (size.value * .32f).sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CallRoundButton(icon: ImageVector, description: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(ComponentSurface).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = PrimaryText, modifier = Modifier.size(size * .48f))
    }
}

@Composable
private fun CallControl(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    disabled: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 82.dp, height = 91.dp)) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(if (destructive) Color(0xFFF5003D) else if (selected) Color(0xFF3A454B) else Color(0xFF20282C))
                .clickable(enabled = !disabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = if (disabled) Color(0xFF465158) else Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = PrimaryText, fontSize = 13.sp)
    }
}

@Composable
private fun ResultAction(icon: ImageVector, label: String, background: Color, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 104.dp, height = 110.dp)) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(background).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(29.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text(label, color = Color.White, fontSize = 13.sp, maxLines = 1)
    }
}
