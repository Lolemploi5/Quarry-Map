package com.example.quarrymap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC5),
    tertiary = Color(0xFF3700B3)
)

@Composable
fun QuarryMapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorPalette, // 🔥 Utilise uniquement la palette sombre
        content = content
    )
}
