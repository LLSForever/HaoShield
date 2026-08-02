package com.haoshield.domain.service

import com.haoshield.domain.model.BlockingMode
import com.haoshield.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/**
 * Where the person's preferences are written down.
 *
 * Deliberately narrower than the platform class behind it: strict-mode bookkeeping stays on the
 * Android side, because only a rooted Android has OS suspensions to keep books about.
 */
interface SettingsStore {
    fun observeBlockingMode(): Flow<BlockingMode>

    suspend fun setBlockingMode(mode: BlockingMode)

    fun observeAmbientSoundEnabled(): Flow<Boolean>

    suspend fun setAmbientSoundEnabled(enabled: Boolean)

    fun observeQuotesEnabled(): Flow<Boolean>

    suspend fun setQuotesEnabled(enabled: Boolean)

    fun observeStrictBlockingEnabled(): Flow<Boolean>

    suspend fun setStrictBlockingEnabled(enabled: Boolean)

    fun observeThemePreference(): Flow<ThemePreference>

    suspend fun setThemePreference(preference: ThemePreference)

    fun observeHasSeenIntro(): Flow<Boolean>

    suspend fun setHasSeenIntro(seen: Boolean)
}
