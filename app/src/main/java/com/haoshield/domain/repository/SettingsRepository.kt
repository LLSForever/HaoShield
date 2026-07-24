package com.haoshield.domain.repository

import com.haoshield.domain.model.BlockingMode
import kotlinx.coroutines.flow.Flow

/**
 * User preferences: the active blocking mode and session defaults.
 */
interface SettingsRepository {
    fun observeBlockingMode(): Flow<BlockingMode>

    suspend fun setBlockingMode(mode: BlockingMode)

    fun observeAmbientSoundEnabled(): Flow<Boolean>

    suspend fun setAmbientSoundEnabled(enabled: Boolean)

    fun observeQuotesEnabled(): Flow<Boolean>

    suspend fun setQuotesEnabled(enabled: Boolean)

    fun observeStrictBlockingEnabled(): Flow<Boolean>

    suspend fun setStrictBlockingEnabled(enabled: Boolean)
}
