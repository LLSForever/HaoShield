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