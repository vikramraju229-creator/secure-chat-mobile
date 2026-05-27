package com.securechat.app.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand Color Palette ──────────────────────────────────────────
// Dark teal & navy background, vibrant cyan/teal accents.

private val DarkTeal    = Color(0xFF0A1C2E)
private val NavyBlue    = Color(0xFF071421)
private val Cyan        = Color(0xFF00BCD4)
private val LightTeal   = Color(0xFF48CFCB)
private val AccentGreen = Color(0xFF00D4AA)
private val SurfaceCard = Color(0xFF122438)
private val SurfaceDark = Color(0xFF0D1B2A)
private val TextWhite   = Color(0xFFFFFFFF)
private val TextMuted   = Color(0xFFB0BEC5)
private val ErrorRed    = Color(0xFFEF5350)
private val ShieldBlue  = Color(0xFF0F2A44)

private val SecureChatColorScheme = darkColorScheme(
    primary        = Cyan,
    onPrimary      = Color.White,
    primaryContainer = LightTeal,
    secondary      = LightTeal,
    onSecondary    = NavyBlue,
    tertiary       = AccentGreen,
    onTertiary     = NavyBlue,

    background     = NavyBlue,
    onBackground   = TextWhite,

    surface        = SurfaceDark,
    onSurface      = TextWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextMuted,

    error          = ErrorRed,
    onError        = Color.White,
    errorContainer = Color(0x33EF5350),
    onErrorContainer = ErrorRed,

    outline        = Color(0xFF264653),
    outlineVariant = Color(0xFF1A3A4A),
)

@Composable
fun SecureChatTheme(
    darkTheme: Boolean = true, // Force dark theme — brand is dark-teal/navy
    content: @Composable () -> Unit
) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = SecureChatColorScheme,
        content = content
    )
}
