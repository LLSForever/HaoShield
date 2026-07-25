package com.haoshield.data.permission

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.haoshield.data.service.accessibility.AppBlockingAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One place to ask whether the Shield can actually run.
 *
 * Blocking needs two system permissions the OS will not let an app grant itself:
 *  - the accessibility service (to notice which app is in the foreground), and
 *  - "display over other apps" (to show the calm protected screen in their place).
 *
 * Without both, a protected session would run its timer but quietly block nothing — so we check
 * these before letting a session begin and guide the user to grant them.
 */
@Singleton
class ShieldPermissions @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isAccessibilityServiceEnabled(): Boolean =
        AppBlockingAccessibilityService.isServiceEnabled(context)

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    /**
     * Whether the session notification can appear. Not required for blocking — a session runs
     * perfectly well without it — so it is deliberately absent from [isReady].
     */
    fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isReady(): Boolean = isAccessibilityServiceEnabled() && canDrawOverlay()
}
