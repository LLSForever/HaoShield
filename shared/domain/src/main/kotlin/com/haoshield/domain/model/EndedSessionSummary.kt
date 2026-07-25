package com.haoshield.domain.model

/**
 * A snapshot of a session that has just ended normally, held briefly so the reflection screen can
 * show how long the time lasted and pair it with the intention it was set aside for. Not recorded
 * for emergency exits, which already capture their own note.
 */
data class EndedSessionSummary(
    val sessionId: Long,
    val mode: SessionMode,
    val durationMillis: Long,
    val intention: String?,
)
