package com.haoshield.domain.model

/** How the app chooses between the light (paper) and dark (dusk) palettes. */
enum class ThemePreference {
    /** Follow the device's light/dark setting. */
    SYSTEM,
    LIGHT,
    DARK,
}
