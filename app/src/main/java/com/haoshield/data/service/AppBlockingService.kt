package com.haoshield.data.service

import kotlinx.coroutines.flow.Flow

/**
 * Accessibility-based app blocking. Implementation will be wired in a later phase.
 */
interface AppBlockingService {
    fun observeIsEnabled(): Flow<Boolean>

    suspend fun isEnabled(): Boolean

    suspend fun setEnabled(enabled: Boolean)
}