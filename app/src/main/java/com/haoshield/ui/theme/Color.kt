package com.haoshield.ui.theme

import androidx.compose.ui.graphics.Color

// Single source of truth for the "Calm" palette. Both HaoShieldTheme's Material3
// color scheme and the individual screens read from these values, so the brand
// palette can be changed in one place. When a dark theme is added, screens can
// migrate to MaterialTheme.colorScheme roles (which these values already back).
val WarmBackground = Color(0xFFF7F5F0)
val Sage = Color(0xFF5C6B5C)
val MutedText = Color(0xFF6B6B68)
val SoftAccent = Color(0xFFE8E4DC)
val Ink = Color(0xFF2C2C2A)
