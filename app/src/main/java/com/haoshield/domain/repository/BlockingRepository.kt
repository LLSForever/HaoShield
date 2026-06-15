package com.haoshield.domain.repository

import com.haoshield.domain.model.BlockedAppGroup
import kotlinx.coroutines.flow.Flow

interface BlockingRepository {
    fun observeBlockedGroups(): Flow<List<BlockedAppGroup>>

    suspend fun getBlockedGroups(): List<BlockedAppGroup>

    suspend fun getBlockedPackageNames(): Set<String>

    suspend fun unblockAppForSession(
        sessionId: Long,
        packageName: String,
        journalNote: String,
    ): Result<Unit>
}