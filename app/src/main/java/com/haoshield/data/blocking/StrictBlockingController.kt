package com.haoshield.data.blocking

import com.haoshield.data.local.BlockedAppsDataStore
import com.haoshield.data.local.SettingsPreferencesDataStore
import com.haoshield.data.root.RootShell
import com.haoshield.domain.di.ApplicationScope
import com.haoshield.domain.service.StrictBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val blockedAppsDataStore: BlockedAppsDataStore,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : StrictBlocking {
    // Per-package re-suspend timers for expired temporary unblocks.
    private val pendingResuspends = mutableMapOf<String, Job>()
    private val resuspendLock = Mutex()

    // The effective blocklist — the user's selection, or the presets until they customise.
    private suspend fun blockedPackages(): List<String> =
        (blockedAppsDataStore.observeBlockedPackages().first()
            ?: PresetBlockedAppGroups.flatMap { it.packageNames }.toSet())
            .toList()

    /** Suspend all blocked packages, if strict mode is enabled and root is available. */
    override suspend fun applyForSession() {
        cancelAllResuspends()
        if (!settings.isStrictBlockingEnabled()) return
        if (!rootShell.isRootBinaryPresent()) return

        val packages = blockedPackages().toSet()
        val ok = rootShell.exec(packages.map { suspendCommand(it) })
        if (ok) {
            // Record exactly what we suspended, so release unsuspends precisely these even if the
            // blocklist is edited during the session.
            settings.setStrictSuspendedPackages(packages)
            settings.setStrictApplied(true)
        }
    }

    /**
     * Release every suspension. Safe to call unconditionally — only does work if we applied.
     * Unsuspends exactly the packages we recorded at apply time (not the current blocklist, which
     * may have been edited). The "applied" flag is cleared ONLY when the unsuspend actually
     * succeeds; otherwise it is left set so a later release retries, rather than stranding apps
     * OS-suspended with no in-app path back.
     */
    override suspend fun releaseAll() {
        cancelAllResuspends()
        if (!settings.isStrictApplied()) return
        val suspended = settings.getStrictSuspendedPackages()
            .ifEmpty { blockedPackages().toSet() }
        val ok = rootShell.exec(suspended.map { unsuspendCommand(it) })
        if (ok) {
            settings.setStrictApplied(false)
            settings.setStrictSuspendedPackages(emptySet())
        }
    }

    /**
     * Release a single package so it can open (unblock with intention). If [resuspendAfterMillis] is
     * given, the app is re-suspended when the allowance expires, so strict mode re-locks it just as
     * the calm boundary would in the non-root case.
     */
    override suspend fun release(packageName: String, resuspendAfterMillis: Long?) {
        if (!settings.isStrictApplied()) return
        rootShell.exec(listOf(unsuspendCommand(packageName)))

        resuspendLock.withLock {
            pendingResuspends.remove(packageName)?.cancel()
            if (resuspendAfterMillis != null) {
                pendingResuspends[packageName] = applicationScope.launch {
                    delay(resuspendAfterMillis)
                    if (settings.isStrictApplied()) {
                        rootShell.exec(listOf(suspendCommand(packageName)))
                    }
                    // The finished Job is left in the map; it's cancelled/cleared on the next
                    // apply/release/releaseAll. Cancelling a completed Job is a harmless no-op.
                }
            }
        }
    }

    private suspend fun cancelAllResuspends() {
        resuspendLock.withLock {
            pendingResuspends.values.forEach { it.cancel() }
            pendingResuspends.clear()
        }
    }

    private fun suspendCommand(packageName: String): String = "pm suspend $packageName"

    private fun unsuspendCommand(packageName: String): String = "pm unsuspend $packageName"
}
