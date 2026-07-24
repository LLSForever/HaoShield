package com.haoshield.data.blocking

import com.haoshield.data.local.SettingsPreferencesDataStore
import com.haoshield.data.root.RootShell
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Strict blocking" — genuine OS-level app suspension on rooted devices, the same mechanism
 * Digital Wellbeing uses. When a session is active, blocked packages are `pm suspend`-ed so they
 * cannot open at all (the system shows an "App is paused" dialog); they are released when the
 * session ends or an app is unblocked with intention.
 *
 * Depends only on [RootShell] and the preferences store — never on SessionManager — so it can be
 * called from within SessionManagerImpl without a DI cycle. The blocked list is read straight from
 * [PresetBlockedAppGroups] (a static constant) for the same reason.
 *
 * A no-op on devices without root, or when the user hasn't enabled strict mode.
 */
@Singleton
class StrictBlockingController @Inject constructor(
    private val rootShell: RootShell,
    private val settings: SettingsPreferencesDataStore,
) {
    private fun blockedPackages(): List<String> =
        PresetBlockedAppGroups.flatMap { it.packageNames }.distinct()

    /** Suspend all blocked packages, if strict mode is enabled and root is available. */
    suspend fun applyForSession() {
        if (!settings.isStrictBlockingEnabled()) return
        if (!rootShell.isRootBinaryPresent()) return

        val ok = rootShell.exec(blockedPackages().map { suspendCommand(it) })
        if (ok) settings.setStrictApplied(true)
    }

    /** Release every suspension. Safe to call unconditionally — only does work if we applied. */
    suspend fun releaseAll() {
        if (!settings.isStrictApplied()) return
        rootShell.exec(blockedPackages().map { unsuspendCommand(it) })
        settings.setStrictApplied(false)
    }

    /** Release a single package, e.g. when it's unblocked with intention so it can open. */
    suspend fun release(packageName: String) {
        if (!settings.isStrictApplied()) return
        rootShell.exec(listOf(unsuspendCommand(packageName)))
    }

    private fun suspendCommand(packageName: String): String = "pm suspend $packageName"

    private fun unsuspendCommand(packageName: String): String = "pm unsuspend $packageName"
}
