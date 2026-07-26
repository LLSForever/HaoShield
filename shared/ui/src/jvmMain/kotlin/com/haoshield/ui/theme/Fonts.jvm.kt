package com.haoshield.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Placeholders until there is a desktop app to render them.
 *
 * The type scale in [DefaultHaoTypography] — the sizes, the leading, the tabular figures — is the
 * part worth sharing, and it is already common. Loading the actual font files here needs
 * compose-resources (or reading them off the classpath), which is worth doing when a desktop window
 * exists to judge the result in, and not before. Falling back to the platform's own serif and sans
 * keeps the desktop target compiling and honest about what it does not yet do.
 *
 * The 好 family deliberately falls back too: [NotoSerifHao] carries only 好 真 善 忍, so a desktop
 * rendering will use whatever CJK face the system provides — the very inconsistency the bundled
 * subset was added to fix on Android. That is a known gap, not a solved one.
 */

actual val NotoSerif: FontFamily = FontFamily.Serif

actual val NotoSerifHao: FontFamily = FontFamily.Serif

actual val Inter: FontFamily = FontFamily.SansSerif
