package com.haoshield.domain.model

enum class SessionEndMethod {
    /** End from within the app. Allowed in Software Mode only. */
    IN_APP,

    /** End by scanning the physical Hǎo Shield. Required to end Shield Mode sessions. */
    SHIELD_SCAN,

    /** Friction-based fallback when the Shield is unavailable. Shield Mode only. */
    EMERGENCY,
}