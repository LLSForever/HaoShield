package com.haoshield.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CalmColorScheme = lightColorScheme(
    primary = Sage,
    onPrimary = Color(0xFFFFFFFF),
    background = WarmBackground,
    onBackground = Ink,
    surface = WarmBackground,
    onSurface = Ink,
    surfaceVariant = SoftAccent,
    onSurfaceVariant = MutedText,
    secondary = MutedText,
)

@Composable
fun HaoShieldTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CalmColorScheme,
        content = content,
    )
}
