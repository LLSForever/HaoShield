package com.haoshield.domain.model

data class Session(
    val id: Long = 0L,
    val mode: SessionMode,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val isActive: Boolean = true,
) {
    val isShieldMode: Boolean
        get() = mode == SessionMode.SHIELD
}