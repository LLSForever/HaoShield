package com.haoshield.domain.usecase

import com.haoshield.domain.repository.BlockingRepository
import javax.inject.Inject

class UnblockAppUseCase @Inject constructor(
    private val blockingRepository: BlockingRepository,
) {
    suspend operator fun invoke(
        sessionId: Long,
        packageName: String,
        journalNote: String,
    ): Result<Unit> {
        if (journalNote.isBlank()) {
            return Result.failure(IllegalArgumentException("Journal note is required to unblock an app."))
        }
        return blockingRepository.unblockAppForSession(
            sessionId = sessionId,
            packageName = packageName,
            journalNote = journalNote,
        )
    }
}