package com.haoshield.domain.model

data class SessionState(
    val session: Session,
    val elapsedMillis: Long,
    val temporarilyAllowedPackages: Set<String> = emptySet(),
) {
    val isShieldMode: Boolean
        get() = session.isShieldMode
}