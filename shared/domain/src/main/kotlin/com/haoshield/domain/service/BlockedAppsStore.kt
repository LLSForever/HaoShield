package com.haoshield.domain.service

import com.haoshield.domain.model.BlockedAppGroup
import kotlinx.coroutines.flow.Flow

/**
 * Where the chosen blocklist is written down.
 *
 * Emits null until the person has customised anything, so callers fall back to the presets. An
 * explicitly empty set (everything removed) is distinct from null and is honoured.
 */
interface BlockedAppsStore {
    fun observeBlockedPackages(): Flow<Set<String>?>

    suspend fun setBlockedPackages(packages: Set<String>)
}

/**
 * The curated groups offered before anyone customises anything.
 *
 * An interface because the list is inherently per-platform: Android package names mean nothing to
 * a desktop, which will offer its own executables and sites.
 */
interface BlockedAppPresets {
    val groups: List<BlockedAppGroup>
}
