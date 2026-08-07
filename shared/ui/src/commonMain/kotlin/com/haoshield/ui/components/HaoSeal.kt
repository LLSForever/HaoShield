package com.haoshield.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.haoshield.domain.Hao
import com.haoshield.ui.theme.HaoTheme
import com.haoshield.ui.theme.NotoSerifHao

/**
 * Sets the 好 character beside each occurrence of the word "Hǎo" in [text] — the name and its
 * own character together, in the manner of a seal pressed beside a signature.
 *
 * The character is set in the app's four-glyph family so it renders identically everywhere, and
 * carries the seal accent by default. The accent appears at most once per screen; a title
 * containing "Hǎo" counts as that once.
 */
@Composable
fun haoSealed(
    text: String,
    sealColor: Color = HaoTheme.colors.seal,
): AnnotatedString = buildAnnotatedString {
    var from = 0
    while (true) {
        val at = text.indexOf(HAO_WORD, from)
        if (at < 0) break
        append(text.substring(from, at + HAO_WORD.length))
        withStyle(
            SpanStyle(
                fontFamily = NotoSerifHao,
                color = sealColor,
                // Matched by eye to the ascender beside it, not by point size: the character's
                // ink fills roughly seven-eighths of its em box where a Latin ascender fills
                // three-quarters, so equal sizes look unequal. At 0.72 the seal read as a
                // footnote to the name; this stands it level with the letters it accompanies.
                fontSize = 0.88.em,
            ),
        ) {
            append(" " + Hao.GOOD)
        }
        from = at + HAO_WORD.length
    }
    append(text.substring(from))
}

private const val HAO_WORD = "Hǎo"
