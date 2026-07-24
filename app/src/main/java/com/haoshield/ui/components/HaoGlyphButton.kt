package com.haoshield.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme

/**
 * The 好 glyph, which is the app's single primary action.
 *
 * At idle it breathes — a barely-perceptible alpha and scale cycle that signals liveness without
 * demanding attention. During an active session the breathing stops and a faint concentric ring
 * sits behind the glyph, with a second ring that slowly expands and fades as a quiet pulse.
 *
 * No ripple (suppressed globally in [HaoTheme]); press feedback is a small scale-down.
 */
@Composable
fun HaoGlyphButton(
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val transition = rememberInfiniteTransition(label = "glyph")

    val breathAlpha by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(HaoMotion.BREATH, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathAlpha",
    )
    val breathScale by transition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(HaoMotion.BREATH, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    // The slow expanding pulse behind an active session. Runs 0 → 1 and restarts.
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = HaoMotion.BREATH * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(HaoMotion.QUICK),
        label = "press",
    )

    val ringColor = HaoTheme.colors.stone
    val pulseColor = HaoTheme.colors.seal
    val glyphAlpha = if (active) 1f else breathAlpha
    val glyphScale = (if (active) 1f else breathScale) * pressScale

    Box(
        modifier = modifier
            .size(GLYPH_TOUCH_TARGET)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription }
            .drawBehind {
                if (!active) return@drawBehind
                val base = RING_RADIUS.toPx()
                // Steady inner ring.
                drawCircle(
                    color = ringColor,
                    radius = base,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
                // Expanding, fading pulse — the seal of a live session, the app's one
                // touch of red while protection is running.
                val pulseRadius = base + pulse * PULSE_TRAVEL.toPx()
                drawCircle(
                    color = pulseColor.copy(alpha = (1f - pulse) * 0.35f),
                    radius = pulseRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "好",
            style = HaoTheme.type.glyph,
            color = HaoTheme.colors.ink,
            modifier = Modifier.graphicsLayer {
                alpha = glyphAlpha
                scaleX = glyphScale
                scaleY = glyphScale
            },
        )
    }
}

// Component-local geometry — no theme tokens exist (or should) for these.
private val GLYPH_TOUCH_TARGET = 200.dp
private val RING_RADIUS = 76.dp
private val PULSE_TRAVEL = 24.dp
