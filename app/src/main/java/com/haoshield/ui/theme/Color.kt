package com.haoshield.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Hǎo Shield palette.
 *
 * Five colours. Resist adding a sixth — if a screen seems to need one,
 * it usually needs less on it instead.
 *
 * Do not reference the raw values below in composables; read them through
 * [HaoTheme.colors] so the whole app stays swappable from one place.
 */

private val Paper       = Color(0xFFF5F3ED) // warm off-white ground
private val PaperRaised = Color(0xFFFAF9F5) // sheets, panels — barely lighter
private val Ink         = Color(0xFF3E4A3D) // primary text, the 好 glyph
private val InkSoft     = Color(0xFF7A8377) // secondary text, subtitles
private val Stone       = Color(0xFFE5E0D6) // callout panels, dividers, inactive
private val SealRed     = Color(0xFF8C4A3F) // single accent — at most once per screen

@Immutable
data class HaoColors(
    val paper: Color,
    val paperRaised: Color,
    val ink: Color,
    val inkSoft: Color,
    val stone: Color,
    val seal: Color,
) {
    /** Text/icon colour that reads correctly on [paper]. */
    val onPaper: Color get() = ink

    /** Text colour for the muted register: subtitles, captions, hints. */
    val onPaperMuted: Color get() = inkSoft

    /** Tertiary register: hints, placeholders, disabled text. Derived, not a sixth colour. */
    val inkFaint: Color get() = inkSoft.copy(alpha = 0.62f)
}

val LightHaoColors = HaoColors(
    paper = Paper,
    paperRaised = PaperRaised,
    ink = Ink,
    inkSoft = InkSoft,
    stone = Stone,
    seal = SealRed,
)

/**
 * Dark theme is deliberately deferred. A calm, paper-like light surface is
 * part of the app's argument. If it is added later, it should be a warm
 * near-black (#1F231E) with the same green ink lifted to ~#A8B5A5 — not an
 * inversion of these values.
 */
