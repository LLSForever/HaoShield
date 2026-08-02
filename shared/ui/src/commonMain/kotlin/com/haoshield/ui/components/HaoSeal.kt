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
                // CJK glyphs run optically larger than Latin at equal size; set slightly under
                // the surrounding type so the seal accompanies the name without shouting it.
                fontSize = 0.72.em,
            ),
        ) {
            append(" " + Hao.GOOD)
        }
        from = at + HAO_WORD.length
    }
    append(text.substring(from))
}

private const val HAO_WORD = "Hǎo"
