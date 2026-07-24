package com.haoshield.data.repository

import com.haoshield.data.blocking.PresetBlockedAppGroups
import com.haoshield.data.local.BlockedAppsDataStore
import com.haoshield.domain.model.BlockedAppGroup
import com.haoshield.domain.repository.BlockingRepository
import com.haoshield.domain.service.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val blockedAppsDataStore: BlockedAppsDataStore,
) : BlockingRepository {

    private val presetPackages: Set<String> =
        PresetBlockedAppGroups.flatMap { it.packageNames }.toSet()

    // The effective blocklist: the user's selection, or the presets until they customise.
    private val effectivePackages: Flow<Set<String>> =
        blockedAppsDataStore.observeBlockedPackages().map { it ?: presetPackages }

    override fun observeBlockedGroups(): Flow<List<BlockedAppGroup>> =
        effectivePackages.map { blocked -> buildGroups(blocked) }

    override suspend fun getBlockedGroups(): List<BlockedAppGroup> =
        buildGroups(effectivePackages.first())

    override suspend fun getBlockedPackageNames(): Set<String> =
        effectivePackages.first()

    override fun observeBlockedPackageNames(): Flow<Set<String>> = effectivePackages

    override fun getPresetGroups(): List<BlockedAppGroup> = PresetBlockedAppGroups

    override suspend fun addBlockedPackage(packageName: String) {
        blockedAppsDataStore.setBlockedPackages(effectivePackages.first() + packageName)
    }

    override suspend fun removeBlockedPackage(packageName: String) {
        blockedAppsDataStore.setBlockedPackages(effectivePackages.first() - packageName)
    }

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

    // Preset groups filtered to what's actually blocked, plus a "Added by you" group for anything
    // the user added beyond the presets. Empty groups are dropped.
    private fun buildGroups(blocked: Set<String>): List<BlockedAppGroup> {
        val presetGroups = PresetBlockedAppGroups.mapNotNull { group ->
            val members = group.packageNames.filter { it in blocked }
            if (members.isEmpty()) null else group.copy(packageNames = members)
        }
        val custom = (blocked - presetPackages).sorted()
        return if (custom.isEmpty()) {
            presetGroups
        } else {
            presetGroups + BlockedAppGroup(
                id = "custom",
                displayName = "Added by you",
                packageNames = custom,
                isPreset = false,
            )
        }
    }
}
