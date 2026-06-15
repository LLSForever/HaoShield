package com.haoshield.data.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.haoshield.data.service.AccessibilityAppBlockingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppBlockingAccessibilityService : AccessibilityService() {

    @Inject lateinit var blockingPolicy: BlockingPolicy

    @Inject lateinit var overlayManager: BlockingOverlayManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastHandledPackage: String? = null
    private var lastHandledAtMillis: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityAppBlockingService.setRunning(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
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
            overlayManager.hide()
            return
        }

        val now = System.currentTimeMillis()
        if (packageName == lastHandledPackage && now - lastHandledAtMillis < DEBOUNCE_MILLIS) {
            return
        }
        lastHandledPackage = packageName
        lastHandledAtMillis = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        overlayManager.show()
        delay(OVERLAY_VISIBLE_MILLIS)
        overlayManager.hide()
    }

    companion object {
        private const val DEBOUNCE_MILLIS = 750L
        private const val OVERLAY_VISIBLE_MILLIS = 2_500L

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