package com.haoshield.domain.usecase

import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.repository.JournalRepository
import javax.inject.Inject

class SaveJournalEntryUseCase @Inject constructor(
    private val journalRepository: JournalRepository,
) {
    suspend operator fun invoke(entry: JournalEntry): JournalEntry =
        journalRepository.saveEntry(entry)
}