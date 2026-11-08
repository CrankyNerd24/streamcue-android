package com.streamcueapp.streamcue.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Same palette as the marketing site and iOS app — see colors.xml.
val StreamCueBackground = Color(0xFF121216)
val StreamCueSurface = Color(0xFF1A1A20)
val StreamCueAccent = Color(0xFFFF9F43)
val StreamCueFree = Color(0xFF3DDC84)
val StreamCueTextPrimary = Color(0xFFF2EFE9)
val StreamCueTextMuted = Color(0xFFA5A2AD)

private val StreamCueColorScheme = darkColorScheme(
    primary = StreamCueAccent,
    background = StreamCueBackground,
    surface = StreamCueSurface,
    onBackground = StreamCueTextPrimary,
    onSurface = StreamCueTextPrimary,
    secondary = StreamCueFree,
)

@Composable
fun StreamCueTheme(content: @Composable () -> Unit) {
    // Dark-only for now, matching the iOS app's .preferredColorScheme(.dark).
    MaterialTheme(
        colorScheme = StreamCueColorScheme,
        content = content
    )
}
