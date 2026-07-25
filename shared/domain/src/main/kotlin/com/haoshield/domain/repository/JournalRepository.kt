package com.haoshield.domain.repository

import com.haoshield.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun observeEntries(): Flow<List<JournalEntry>>

    suspend fun getEntry(id: Long): JournalEntry?

    suspend fun saveEntry(entry: JournalEntry): JournalEntry
}