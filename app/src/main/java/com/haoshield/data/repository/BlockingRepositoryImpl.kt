package com.haoshield.data.repository

import com.haoshield.data.blocking.PresetBlockedAppGroups
import com.haoshield.domain.model.BlockedAppGroup
import com.haoshield.domain.repository.BlockingRepository
import com.haoshield.domain.service.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : BlockingRepository {
    private val blockedGroups = MutableStateFlow<List<BlockedAppGroup>>(PresetBlockedAppGroups)

    override fun observeBlockedGroups(): Flow<List<BlockedAppGroup>> =
        blockedGroups.asStateFlow()

    override suspend fun getBlockedGroups(): List<BlockedAppGroup> =
        blockedGroups.value

    override suspend fun getBlockedPackageNames(): Set<String> =
        blockedGroups.value
            .flatMap { it.packageNames }
            .toSet()

    override suspend fun unblockAppForSession(
        sessionId: Long,
        packageName: String,
        journalNote: String,
    ): Result<Unit> {
        val activeSession = sessionManager.getActiveSession()
            ?: return Result.failure(IllegalStateException("No active session."))
        if (activeSession.id != sessionId) {
            return Result.failure(IllegalStateException("Session mismatch."))
        }
        return sessionManager.allowAppTemporarily(packageName, journalNote)
    }
}