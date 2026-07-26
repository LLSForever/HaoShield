package com.haoshield.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme

/**
 * The app's two shared button voices. Primary = one filled, decisive action per screen.
 * Secondary = a visible but quiet alternative. Both use the glyph's press idiom
 * (small scale-down, no ripple) so every press in the app feels the same.
 */

@Composable
fun HaoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(HaoMotion.QUICK),
        label = "press",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = HaoTheme.shapes.button,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = HaoTheme.colors.ink,
            contentColor = HaoTheme.colors.paper,
            disabledContainerColor = HaoTheme.colors.stone,
            disabledContentColor = HaoTheme.colors.inkSoft,
        ),
        modifier = modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        },
    ) {
        Text(text = text, style = HaoTheme.type.body)
    }
}

@Composable
fun HaoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(HaoMotion.QUICK),
        label = "press",
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = HaoTheme.shapes.button,
        interactionSource = interactionSource,
        border = BorderStroke(1.dp, HaoTheme.colors.stone),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = HaoTheme.colors.ink,
            disabledContentColor = HaoTheme.colors.inkFaint,
        ),
        modifier = modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        },
    ) {
        Text(text = text, style = HaoTheme.type.body)
    }
}
