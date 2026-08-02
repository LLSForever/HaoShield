package com.haoshield.domain.repository

import com.haoshield.domain.model.BlockedAppGroup
import kotlinx.coroutines.flow.Flow

interface BlockingRepository {
    fun observeBlockedGroups(): Flow<List<BlockedAppGroup>>

    suspend fun getBlockedGroups(): List<BlockedAppGroup>

    suspend fun getBlockedPackageNames(): Set<String>

    /** The effective blocklist as a live set, for the editor to reflect and toggle. */
    fun observeBlockedPackageNames(): Flow<Set<String>>

    /** The curated preset groups, regardless of the user's current selection. */
    fun getPresetGroups(): List<BlockedAppGroup>

    suspend fun addBlockedPackage(packageName: String)

    suspend fun removeBlockedPackage(packageName: String)

    suspend fun unblockAppForSession(
        sessionId: Long,
        packageName: String,
        journalNote: String,
    ): Result<Unit>
}