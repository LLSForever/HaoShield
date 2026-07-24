package com.haoshield.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 8pt grid. The larger steps exist because this app should feel emptier
 * than a normal app — reach for [xl] and [xxl] more often than feels natural.
 */
@Immutable
data class HaoSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 40.dp,
    val xxl: Dp = 64.dp,
    /** Standard horizontal page inset. */
    val screenH: Dp = 28.dp,
)

/**
 * Soft, not round. Nothing here is a pill — pills read as playful/commercial.
 */
@Immutable
data class HaoShapes(
    val panel: Shape = RoundedCornerShape(4.dp),
    val card: Shape = RoundedCornerShape(8.dp),
    val button: Shape = RoundedCornerShape(6.dp),
)

/**
 * Motion. Everything is slower than Material's defaults, on purpose.
 */
object HaoMotion {
    const val QUICK = 240      // press feedback, toggles
    const val STANDARD = 450   // screen content fades, expansions
    const val SLOW = 700       // entering/leaving a session
    const val GENTLE = 2000    // quote fades, ambient reveals
    const val BREATH = 4200    // idle glyph breathing half-cycle
}
