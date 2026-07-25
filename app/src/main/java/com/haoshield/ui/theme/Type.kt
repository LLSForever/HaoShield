package com.haoshield.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.haoshield.R

/**
 * Two families, a small set of sizes.
 *
 * Serif carries the voice of the app (headings, the glyph, quotes).
 * Sans carries information (body, labels, settings).
 *
 * Both are variable OFL fonts (single file per family); the weight axis is
 * selected per style below. Variable fonts are supported on API 26+, which is
 * our minSdk.
 *
 * The 好 glyph has its own family. Noto Serif (Latin) contains no CJK, so the
 * character used to render via whatever serif-CJK fallback the device happened
 * to ship — which meant the app, the launcher icon and the printed sheet could
 * each show a different 好. [NotoSerifHao] is Noto Serif SC (OFL) subset down to
 * that single glyph, ~2KB, so it is now identical everywhere. It contains ONLY
 * 好: never use it for text.
 */

// Variable fonts: the requested FontWeight maps to the font's wght axis on API 26+ (our minSdk).
private fun notoSerif(weight: FontWeight) = Font(resId = R.font.noto_serif_variable, weight = weight)

private fun inter(weight: FontWeight) = Font(resId = R.font.inter_variable, weight = weight)

val NotoSerif = FontFamily(
    notoSerif(FontWeight.Normal),
    notoSerif(FontWeight.Medium),
)

/** Single-glyph family: 好 and nothing else. Licence in assets/licenses/NotoSerifSC-OFL.txt. */
val NotoSerifHao = FontFamily(Font(resId = R.font.noto_serif_hao))

val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
)

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

@Immutable
data class HaoTypography(
    /** The 好 glyph itself. Size is set at the call site; this carries family only. */
    val glyph: TextStyle,
    /** A smaller 好 for heroes, stamps, and empty states. */
    val glyphSmall: TextStyle,
    /** 好 as a quiet mark accompanying other content, rather than as the hero. */
    val glyphMark: TextStyle,
    /** The session clock and emergency countdown digits. */
    val timer: TextStyle,
    /** The quiet running session clock. Smaller than [timer] — it sits with you, it doesn't count. */
    val clock: TextStyle,
    /** Screen titles: "Hǎo Shield", "Make Your Own Shield". */
    val display: TextStyle,
    /** Section headings within a screen. */
    val heading: TextStyle,
    /** Contemplative quotes, session messages — the app speaking. */
    val voice: TextStyle,
    /** Standard reading text. */
    val body: TextStyle,
    /** Subtitles, hints, secondary rows. */
    val caption: TextStyle,
    /** Small all-caps section labels. Used sparingly. */
    val label: TextStyle,
)

val DefaultHaoTypography = HaoTypography(
    glyph = TextStyle(
        fontFamily = NotoSerifHao,
        fontWeight = FontWeight.Normal,
        fontSize = 96.sp,
        lineHeight = 112.sp,
        lineHeightStyle = Trim,
    ),
    glyphSmall = TextStyle(
        fontFamily = NotoSerifHao,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp,
        lineHeight = 84.sp,
        lineHeightStyle = Trim,
    ),
    glyphMark = TextStyle(
        fontFamily = NotoSerifHao,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        lineHeightStyle = Trim,
    ),
    timer = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp,
        lineHeight = 84.sp,
        letterSpacing = 2.sp,
        lineHeightStyle = Trim,
    ),
    clock = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = 1.sp,
        lineHeightStyle = Trim,
    ),
    display = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = Trim,
    ),
    heading = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        lineHeightStyle = Trim,
    ),
    voice = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 36.sp, // 1.6 — deliberately loose
        lineHeightStyle = Trim,
    ),
    body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        lineHeightStyle = Trim,
    ),
    caption = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        lineHeightStyle = Trim,
    ),
    label = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
        lineHeightStyle = Trim,
    ),
)
