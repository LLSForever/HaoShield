package com.haoshield.domain.model

data class SessionState(
    val session: Session,
    val elapsedMillis: Long,
    /** Package name → epoch millis at which the temporary allowance expires and blocking resumes. */
    val temporarilyAllowedPackages: Map<String, Long> = emptyMap(),
) {
    val isShieldMode: Boolean
        get() = session.isShieldMode
}