package com.haoshield.domain.model

enum class JournalEntryType {
    UNBLOCK,
    REFLECTION,
    EMERGENCY_EXIT,
}

data class JournalEntry(
    val id: Long = 0L,
    val content: String,
    val createdAtEpochMillis: Long,
    val sessionId: Long? = null,
    val type: JournalEntryType = JournalEntryType.REFLECTION,
    val unblockedPackageName: String? = null,
)