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

// Dark ("dusk") palette. Not an inversion: the ground is a warm near-black with green
// in it, and the ink is the same green lifted until it reads — lamplight on paper,
// not a black screen with white text. Never pure #000 or #FFF.
private val Dusk         = Color(0xFF1F231E) // warm near-black ground
private val DuskRaised   = Color(0xFF262B24) // sheets, panels — barely lighter
private val InkLifted    = Color(0xFFA8B5A5) // primary text, the 好 glyph
private val InkLiftedSoft= Color(0xFF7C8A79) // secondary text, subtitles
private val StoneDark    = Color(0xFF333A31) // callout panels, dividers, inactive
private val SealRedLifted= Color(0xFFC0705C) // the accent, warmed so it carries on dusk

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
 * The same five roles at dusk. Kept as a parallel set rather than a computed
 * inversion so each value can be judged by eye against the others.
 *
 * If this palette changes, mirror it in res/values-night/colors.xml — those
 * back the blocking overlay, which is a plain View layout and cannot read these.
 */
val DarkHaoColors = HaoColors(
    paper = Dusk,
    paperRaised = DuskRaised,
    ink = InkLifted,
    inkSoft = InkLiftedSoft,
    stone = StoneDark,
    seal = SealRedLifted,
)
