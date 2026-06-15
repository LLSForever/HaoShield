package com.haoshield.data.local.room

import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalEntryType

fun JournalEntryEntity.toDomain(): JournalEntry =
    JournalEntry(
        id = id,
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
        sessionId = sessionId,
        type = runCatching { JournalEntryType.valueOf(type) }.getOrDefault(JournalEntryType.REFLECTION),
        unblockedPackageName = unblockedPackageName,
    )

fun JournalEntry.toEntity(): JournalEntryEntity =
    JournalEntryEntity(
        id = id,
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
        sessionId = sessionId,
        type = type.name,
        unblockedPackageName = unblockedPackageName,
    )