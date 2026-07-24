package com.haoshield.data.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.haoshield.MainActivity
import com.haoshield.data.service.AccessibilityAppBlockingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppBlockingAccessibilityService : AccessibilityService() {

    @Inject lateinit var blockingPolicy: BlockingPolicy

    @Inject lateinit var overlayManager: BlockingOverlayManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityAppBlockingService.setRunning(true)
    }

    // The package the boundary is currently covering, so we don't churn show/hide on every event.
    @Volatile private var coveredPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val packageName = resolveForegroundPackage(event) ?: return

        // Only a definitive foreground change may DISMISS the boundary. TYPE_WINDOWS_CHANGED fires
        // constantly during launch animations (briefly resolving to the launcher, etc.); letting it
        // hide caused the boundary to flicker off and on. It may only re-assert (show), which is what
        // makes returning to a blocked app via recents re-cover it.
        val canHide = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

        serviceScope.launch {
            handleForegroundPackage(packageName, canHide)
        }
    }

    /**
     * The real foreground app. [AccessibilityEvent.getPackageName] is reliable for
     * TYPE_WINDOW_STATE_CHANGED but is often null for TYPE_WINDOWS_CHANGED (which is what fires when
     * you return to an already-running app via the recents switcher), so we fall back to the active
     * window's package there.
     */
    private fun resolveForegroundPackage(event: AccessibilityEvent): String? {
        val fromEvent = event.packageName?.toString()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            !fromEvent.isNullOrBlank()
        ) {
            return fromEvent
        }
        return runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
            ?: fromEvent
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        hideBoundary()
        AccessibilityAppBlockingService.setRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleForegroundPackage(packageName: String, canHide: Boolean) {
        when {
            // Our own windows (protected overlay, unblock screen) manage the boundary explicitly.
            packageName == applicationContext.packageName -> return
            // Recents / notification shade / transient system windows: keep the boundary up so a
            // blocked app isn't exposed underneath. Tearing it down here is how the app-switch
            // escape happened — the overlay vanished and never re-appeared on return.
            packageName in TRANSIENT_SYSTEM_PACKAGES -> return
            blockingPolicy.shouldBlock(packageName) -> showBoundary(packageName)
            // Launcher or an allowed app: the user has genuinely left the blocked app — but only
            // dismiss on a definitive foreground change, never on transient window churn.
            canHide -> hideBoundary()
        }
    }

    private fun showBoundary(packageName: String) {
        if (!overlayManager.canDrawOverlay()) {
            // Without overlay permission we can't show the calm screen; at least step the user away.
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        coveredPackage = packageName
        // The callbacks read coveredPackage live rather than capturing [packageName], so that if the
        // same overlay is reused across a blocked→blocked switch, Unblock still targets the app the
        // user is actually looking at — not the one the overlay was first created for.
        overlayManager.show(
            onUnblock = {
                val target = coveredPackage
                hideBoundary()
                target?.let { launchUnblock(it) }
            },
            onStepAway = {
                hideBoundary()
                performGlobalAction(GLOBAL_ACTION_HOME)
            },
        )
    }

    private fun hideBoundary() {
        coveredPackage = null
        overlayManager.hide()
    }

    private fun launchUnblock(packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_UNBLOCK_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    companion object {
        // Windows that appear during transitions (recents, notification shade, status bar). Landing
        // on one must NOT dismiss the boundary, or the blocked app underneath becomes reachable.
        private val TRANSIENT_SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "android",
        )

        fun isServiceEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false

            val component = ComponentName(context, AppBlockingAccessibilityService::class.java)
            return enabledServices.split(':').any { it.equals(component.flattenToString(), ignoreCase = true) }
        }
    }
}
