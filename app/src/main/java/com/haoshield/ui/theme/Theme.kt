package com.haoshield.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CalmColorScheme = lightColorScheme(
    primary = Color(0xFF5C6B5C),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF2C2C2A),
    surface = Color(0xFFF7F5F0),
    onSurface = Color(0xFF2C2C2A),
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