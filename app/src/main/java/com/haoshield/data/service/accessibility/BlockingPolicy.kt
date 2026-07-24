package com.haoshield.data.service.accessibility

import android.content.Context
import com.haoshield.domain.repository.BlockingRepository
import com.haoshield.domain.service.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingPolicy @Inject constructor(
    private val blockingRepository: BlockingRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
) {
    suspend fun shouldBlock(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == context.packageName) return false
        if (packageName in IGNORED_PACKAGES) return false

        // Ensure a session persisted across a process restart has been restored before we decide;
        // otherwise a blocked app opened moments after restart would read null and slip through.
        sessionManager.awaitRestored()
        sessionManager.getActiveSession() ?: return false
        if (sessionManager.isAppTemporarilyAllowed(packageName)) return false

        return packageName in blockingRepository.getBlockedPackageNames()
    }

    /**
     * Millis until [packageName]'s temporary allowance expires and it becomes blockable again, or
     * null if it isn't a blocked app on a live allowance. The service uses this to schedule the
     * boundary's return even while the user is still sitting inside the unblocked app.
     */
    suspend fun temporaryAllowanceRemainingMillis(packageName: String): Long? {
        if (packageName !in blockingRepository.getBlockedPackageNames()) return null
        val expiry = sessionManager.getTemporaryAllowanceExpiry(packageName) ?: return null
        return (expiry - System.currentTimeMillis()).takeIf { it > 0 }
    }

    private companion object {
        val IGNORED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
        )
    }
}