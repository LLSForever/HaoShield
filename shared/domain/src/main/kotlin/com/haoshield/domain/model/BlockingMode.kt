package com.haoshield.domain.model

/**
 * How a protected session is begun and ended.
 *
 * - [SOFTWARE]: a lighter way to begin — tap the 好 glyph to start, end from within the app.
 * - [SHIELD]: stronger friction — a registered physical Hǎo Shield (NFC tag or printed QR) starts
 *   and ends the session.
 *
 * This is a user preference chosen in Settings, not a per-session choice on the home screen.
 */
enum class BlockingMode {
    SOFTWARE,
    SHIELD,
}
