package com.jp.whatsappclone.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF0B1014)
val ElevatedSurface = Color(0xFF13181C)
val SearchSurface = Color(0xFF21262A)
val ComponentSurface = Color(0xFF1F272A)
val IncomingBubble = Color(0xFF1F272A)
val OutgoingBubble = Color(0xFF134D37)
val SelectedPill = Color(0xFF242625)
val PrimaryText = Color(0xFFE9EDEF)
val SecondaryText = Color(0xFF8696A0)
val BrandGreen = Color(0xFF25D366)
val AttachmentGreen = Color(0xFF08CE9D)
val ReadBlue = Color(0xFF53BDEB)
val DestructiveRed = Color(0xFFF15C6D)
val ActionWhite = Color(0xFFFAFAFA)
val DividerColor = Color(0xFF20272B)

private val WhatsAppDarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = AppBackground,
    secondary = AttachmentGreen,
    background = AppBackground,
    onBackground = PrimaryText,
    surface = ElevatedSurface,
    onSurface = PrimaryText,
    surfaceVariant = ComponentSurface,
    onSurfaceVariant = SecondaryText,
    error = DestructiveRed,
)

@Composable
fun WhatsAppCloneTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = WhatsAppDarkColors,
        typography = WhatsAppTypography,
        content = content,
    )
}
