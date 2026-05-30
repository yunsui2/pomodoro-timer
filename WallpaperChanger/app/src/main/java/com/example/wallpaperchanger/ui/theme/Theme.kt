package com.example.wallpaperchanger.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF334155)
)

@Composable
fun WallpaperChangerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
