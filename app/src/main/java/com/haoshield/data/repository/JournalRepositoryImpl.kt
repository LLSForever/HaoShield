package com.haoshield.data.repository

import com.haoshield.data.local.room.JournalEntryDao
import com.haoshield.data.local.room.toDomain
import com.haoshield.data.local.room.toEntity
import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val journalEntryDao: JournalEntryDao,
) : JournalRepository {

    override fun observeEntries(): Flow<List<JournalEntry>> =
        journalEntryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getEntry(id: Long): JournalEntry? =
        journalEntryDao.getById(id)?.toDomain()

    override suspend fun saveEntry(entry: JournalEntry): JournalEntry {
        return if (entry.id == 0L) {
            val insertedId = journalEntryDao.insert(entry.toEntity())
            journalEntryDao.getById(insertedId)?.toDomain()
                ?: entry.copy(id = insertedId)
        } else {
            journalEntryDao.update(entry.toEntity())
            journalEntryDao.getById(entry.id)?.toDomain() ?: entry
        }
    }
}