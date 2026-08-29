package com.quicksshtunnel.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark developer-tool palette
val BgColor = Color(0xFF1A1A1A)
val SurfaceColor = Color(0xFF242424)
val ActiveColor = Color(0xFF4CAF50)
val InactiveColor = Color(0xFF888888)
val ErrorColor = Color(0xFFF44336)
val BlueColor = Color(0xFF2196F3)
val TextPrimary = Color(0xFFE0E0E0)
val TextSecondary = Color(0xFF888888)
val SurfaceVariant = Color(0xFF2E2E2E)
val OutlineColor = Color(0xFF333333)

private val DarkColors = darkColorScheme(
    primary = ActiveColor,
    onPrimary = Color.Black,
    secondary = BlueColor,
    onSecondary = Color.White,
    background = BgColor,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    onError = Color.White,
    outline = OutlineColor,
)

@Composable
fun QuickSshTunnelTheme(
    content: @Composable () -> Unit,
) {
    // Always dark — this is a dev tool
    MaterialTheme(
        colorScheme = DarkColors,

        content = content,
    )
}
