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
import androidx.compose.ui.unit.dp
import com.haoshield.ui.theme.HaoTheme

/**
 * The single back affordance: top-left, quiet, caption-sized. Label policy:
 * "Back" for hierarchical navigation, "Cancel" for abandoning a flow, "Not now" for
 * deferring setup. No bottom back buttons anywhere.
 *
 * Deliberately not a [androidx.compose.material3.TextButton]: that applies Material's own
 * content padding, which set the label 12dp right of the screen margin so it never lined up with
 * the title and body beneath it. Here the text sits flush on the margin and the touch target is
 * grown downward and to the right instead, where it costs no alignment.
 */
@Composable
fun HaoBackLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Back",
) {
    Box(
        modifier = modifier
            .padding(top = HaoTheme.spacing.sm)
            .heightIn(min = MIN_TOUCH_TARGET)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = label,
                onClick = onClick,
            )
            // Widens the target without moving the label off the margin.
            .padding(end = HaoTheme.spacing.xl),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = label, style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
    }
}

private val MIN_TOUCH_TARGET = 48.dp
