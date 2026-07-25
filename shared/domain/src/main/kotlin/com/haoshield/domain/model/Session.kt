package com.haoshield.domain.model

data class Session(
    val id: Long = 0L,
    val mode: SessionMode,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val isActive: Boolean = true,
    /** What the person set this protected time aside for. Optional, offered gently after start. */
    val intention: String? = null,
) {
    val isShieldMode: Boolean
        get() = mode == SessionMode.SHIELD
}