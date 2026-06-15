package com.haoshield.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val content: String,
    val createdAtEpochMillis: Long,
    val sessionId: Long?,
    val type: String,
    val unblockedPackageName: String?,
)