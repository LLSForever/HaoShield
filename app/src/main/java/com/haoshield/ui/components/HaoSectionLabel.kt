package com.haoshield.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haoshield.ui.theme.HaoTheme

/** Small all-caps section label; optional stone hairline above for section separation. */
@Composable
fun HaoSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    topDivider: Boolean = false,
) {
    if (topDivider) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HaoTheme.spacing.xl),
            thickness = 1.dp,
            color = HaoTheme.colors.stone,
        )
    }
    Text(
        text = text,
        style = HaoTheme.type.label,
        color = HaoTheme.colors.inkSoft,
        modifier = modifier.padding(
            top = if (topDivider) HaoTheme.spacing.lg else HaoTheme.spacing.xl,
            bottom = HaoTheme.spacing.sm,
        ),
    )
}
