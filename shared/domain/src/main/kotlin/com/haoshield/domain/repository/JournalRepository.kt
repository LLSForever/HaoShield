package com.haoshield.domain.repository

import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalRetention
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun observeEntries(): Flow<List<JournalEntry>>

    suspend fun getEntry(id: Long): JournalEntry?

    suspend fun saveEntry(entry: JournalEntry): JournalEntry

    /** Let go of entries written before [cutoffEpochMillis]. See [JournalRetention]. */
    suspend fun pruneEntriesBefore(cutoffEpochMillis: Long)
}