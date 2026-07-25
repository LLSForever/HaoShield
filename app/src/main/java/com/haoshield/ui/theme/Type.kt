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
 * Two families, a small set of sizes, set like a book rather than a dashboard.
 *
 * Serif carries the voice of the app (headings, the glyph, quotes).
 * Sans carries information (body, labels, settings).
 *
 * The reading sizes sit on the traditional printers' scale — the sequence
 * (…, 12, 14, 16, 18, 21, 24, 36, 48, 60, 72) that book typography settled on
 * over five centuries, for the same reason musical intervals settled: steps
 * that are unmistakably distinct without shouting. Leading follows classical
 * practice: body text at one-and-a-half (the canonical book ratio), headings
 * and display tighter (large type needs less air), small text and the app's
 * voice looser. All leadings are multiples of 4sp, so text and the 8pt spacing
 * grid share a vertical rhythm.
 *
 * The numeric display cuts (clock, timer) are the exception, sized by eye —
 * which is itself the tradition; display type was always cut optically. They
 * request tabular figures so the digits sit in fixed columns and a ticking
 * second doesn't make the line shimmy.
 *
 * Both text families are variable OFL fonts (single file per family); the
 * weight axis is selected per style below. Variable fonts are supported on
 * API 26+, which is our minSdk.
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
        fontSize = 36.sp,
        lineHeight = 44.sp,
        lineHeightStyle = Trim,
    ),
    timer = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 72.sp,
        lineHeight = 84.sp,
        letterSpacing = 2.sp,
        // Fixed-width digits: the countdown must not shimmy as it falls.
        fontFeatureSettings = "tnum",
        lineHeightStyle = Trim,
    ),
    clock = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = 1.sp,
        // Fixed-width digits: a ticking second shouldn't nudge the minutes around.
        fontFeatureSettings = "tnum",
        lineHeightStyle = Trim,
    ),
    display = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp, // 1.22 — display type carries less air
        letterSpacing = (-0.4).sp, // large serif tracks slightly tight
        lineHeightStyle = Trim,
    ),
    heading = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp, // 1.33 — headings lead tighter than text
        lineHeightStyle = Trim,
    ),
    voice = TextStyle(
        fontFamily = NotoSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
        lineHeight = 32.sp, // 1.52 — deliberately loose; the app speaking slowly
        lineHeightStyle = Trim,
    ),
    body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp, // 1.5 — the canonical book ratio
        lineHeightStyle = Trim,
    ),
    caption = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp, // 1.43 — small text, slightly looser than body's ratio at size
        lineHeightStyle = Trim,
    ),
    label = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp, // 0.1em — the classical rule for letterspaced capitals
        lineHeightStyle = Trim,
    ),
)
