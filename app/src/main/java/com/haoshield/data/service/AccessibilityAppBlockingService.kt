package com.haoshield.data.service

import android.content.Context
import com.haoshield.data.service.accessibility.AppBlockingAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityAppBlockingService @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppBlockingService {

    override fun observeIsEnabled(): Flow<Boolean> = isRunning.asStateFlow()

    override suspend fun isEnabled(): Boolean =
        AppBlockingAccessibilityService.isServiceEnabled(context) && isRunning.value

    override suspend fun setEnabled(enabled: Boolean) {
        // Accessibility must be toggled in system settings by the user.
        if (!enabled) {
            isRunning.value = false
        }
    }

    companion object {
        private val isRunning = MutableStateFlow(false)

        fun setRunning(running: Boolean) {
            isRunning.value = running
        }
    }
}