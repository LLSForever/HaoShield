package com.haoshield.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.haoshield.ui.theme.HaoTheme

/** A quiet radio/checkbox mark: a stone ring, filled with ink when selected. */
@Composable
fun HaoSelectionDot(selected: Boolean, modifier: Modifier = Modifier) {
    val ring = HaoTheme.colors.inkSoft
    val fill = HaoTheme.colors.ink
    Spacer(
        modifier = modifier
            .size(18.dp)
            .drawBehind {
                drawCircle(color = ring, radius = size.minDimension / 2f, style = Stroke(1.5.dp.toPx()))
                if (selected) {
                    drawCircle(color = fill, radius = size.minDimension / 4f)
                }
            },
    )
}
