package com.haoshield.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.haoshield.ui.theme.HaoTheme

/**
 * An inline text action that keeps the page's alignment.
 *
 * Material's TextButton pads its label 12dp inward, which pushed every such link off the margin
 * the surrounding text sits on — the tell that a screen is assembled from widgets rather than
 * set like a page. Here the label sits flush with the text around it, and the touch target grows
 * in the directions that cost nothing: downward, and away from the aligned edge.
 */
@Composable
fun HaoTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = HaoTheme.type.label,
    color: Color = HaoTheme.colors.ink,
    alignEnd: Boolean = false,
) {
    Box(
        modifier = modifier
            .heightIn(min = MIN_TOUCH_TARGET)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = text,
                onClick = onClick,
            )
            // Widen the target away from whichever edge the label is aligned to.
            .padding(
                start = if (alignEnd) HaoTheme.spacing.xl else 0.dp,
                end = if (alignEnd) 0.dp else HaoTheme.spacing.xl,
            ),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(text = text, style = style, color = color)
    }
}

private val MIN_TOUCH_TARGET = 44.dp
