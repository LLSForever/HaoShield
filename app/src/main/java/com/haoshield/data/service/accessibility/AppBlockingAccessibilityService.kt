package com.haoshield.data.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        // Ignore our own windows (including the protected overlay) so the boundary never dismisses
        // itself when the overlay or the unblock screen appears.
        if (packageName == applicationContext.packageName) return

        serviceScope.launch {
            handleForegroundPackage(packageName)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        overlayManager.hide()
        AccessibilityAppBlockingService.setRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleForegroundPackage(packageName: String) {
        if (!blockingPolicy.shouldBlock(packageName)) {
            // Dismissing a resting app lands on the home screen (or another system surface) with
            // the shield still up — that's expected, so those surfaces don't take the shield down.
            // Any real app coming forward reclaims the screen.
            if (!(overlayManager.isShowing && isSystemSurface(packageName))) {
                overlayManager.hide()
            }
            return
        }

        if (overlayManager.canDrawOverlay()) {
            overlayManager.show(
                onUnblock = {
                    overlayManager.hide()
                    launchUnblock(packageName)
                },
                onStepAway = {
                    overlayManager.hide()
                    performGlobalAction(GLOBAL_ACTION_HOME)
                },
            )
        }

        // Dismiss the resting app instead of leaving it running behind the shield, where the app
        // switcher would still expose its live content.
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun isSystemSurface(packageName: String): Boolean {
        if (packageName == "android" || packageName == "com.android.systemui") return true
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .any { it.activityInfo.packageName == packageName }
    }

    private fun launchUnblock(packageName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_UNBLOCK_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    companion object {
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
