package com.haoshield.data.service

import kotlinx.coroutines.flow.Flow

/**
 * Accessibility-based app blocking. Implemented by [com.haoshield.data.service.AccessibilityAppBlockingService],
 * which tracks whether the [AppBlockingAccessibilityService] is currently connected.
 */
interface AppBlockingService {
    fun observeIsEnabled(): Flow<Boolean>

    suspend fun isEnabled(): Boolean

    suspend fun setEnabled(enabled: Boolean)
}