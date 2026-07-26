package com.haoshield.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Custom design system layered over M3.
 *
 * Why both: M3 is kept underneath so standard components (Switch, Slider,
 * TextField, Scaffold, Snackbar) still work and stay accessible, but every
 * colour and text style in Hǎo Shield's own screens comes from [HaoTheme],
 * not MaterialTheme. Read tokens as HaoTheme.colors.ink, HaoTheme.type.body, etc.
 *
 * Rule for the codebase: no hardcoded Color(...) or .sp values outside this
 * package. If something needs a value that isn't here, add it here first.
 */

val LocalHaoColors = staticCompositionLocalOf { LightHaoColors }
val LocalHaoTypography = staticCompositionLocalOf { DefaultHaoTypography }
val LocalHaoSpacing = staticCompositionLocalOf { HaoSpacing() }
val LocalHaoShapes = staticCompositionLocalOf { HaoShapes() }

object HaoTheme {
    val colors: HaoColors
        @Composable @ReadOnlyComposable get() = LocalHaoColors.current

    val type: HaoTypography
        @Composable @ReadOnlyComposable get() = LocalHaoTypography.current

    val spacing: HaoSpacing
        @Composable @ReadOnlyComposable get() = LocalHaoSpacing.current

    val shapes: HaoShapes
        @Composable @ReadOnlyComposable get() = LocalHaoShapes.current
}

/**
 * Ripples are suppressed almost to nothing. A visible ripple is the single
 * biggest tell that an app is "a Material app" — and it fights the stillness
 * this app is trying to hold. Press feedback is done with scale/alpha instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
private val QuietRipple = RippleConfiguration(
    color = Color.Unspecified,
    rippleAlpha = null,
)

/** M3 scheme derived from the Hǎo palette so stock components don't jar. */
private fun materialSchemeFrom(c: HaoColors, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = c.ink,
        onPrimary = c.paper,
        secondary = c.inkSoft,
        onSecondary = c.paper,
        tertiary = c.seal,
        onTertiary = c.paper,
        background = c.paper,
        onBackground = c.ink,
        surface = c.paper,
        onSurface = c.ink,
        surfaceVariant = c.stone,
        onSurfaceVariant = c.inkSoft,
        outline = c.stone,
        outlineVariant = c.stone,
        error = c.seal,
        onError = c.paper,
    )
} else {
    lightColorScheme(
        primary = c.ink,
        onPrimary = c.paper,
        secondary = c.inkSoft,
        onSecondary = c.paper,
        tertiary = c.seal,
        onTertiary = c.paper,
        background = c.paper,
        onBackground = c.ink,
        surface = c.paper,
        onSurface = c.ink,
        surfaceVariant = c.stone,
        onSurfaceVariant = c.inkSoft,
        outline = c.stone,
        outlineVariant = c.stone,
        error = c.seal,
        onError = c.paper,
    )
}

/** True when the dusk palette is in use — for system-bar icon appearance, etc. */
val LocalHaoIsDark = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaoTheme(
    dark: Boolean = false,
    colors: HaoColors = if (dark) DarkHaoColors else LightHaoColors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHaoColors provides colors,
        LocalHaoIsDark provides dark,
        LocalHaoTypography provides DefaultHaoTypography,
        LocalHaoSpacing provides HaoSpacing(),
        LocalHaoShapes provides HaoShapes(),
        LocalRippleConfiguration provides QuietRipple,
    ) {
        MaterialTheme(colorScheme = materialSchemeFrom(colors, dark)) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.paper),
                color = colors.paper,
                content = content,
            )
        }
    }
}
