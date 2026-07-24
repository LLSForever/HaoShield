package com.haoshield.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haoshield.ui.theme.HaoTheme

/**
 * The single back affordance: top-left, quiet, caption-sized. Label policy:
 * "Back" for hierarchical navigation, "Cancel" for abandoning a flow, "Not now" for
 * deferring setup. No bottom back buttons anywhere.
 */
@Composable
fun HaoBackLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Back",
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.padding(top = HaoTheme.spacing.sm),
    ) {
        Text(text = label, style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
    }
}
