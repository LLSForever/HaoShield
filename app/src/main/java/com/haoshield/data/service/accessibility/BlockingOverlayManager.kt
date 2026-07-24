package com.haoshield.data.service.accessibility

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.haoshield.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Whether the cached view is currently attached to the window manager.
    private var attached: Boolean = false

    // The overlay view is inflated once and reused. Re-inflating on every block added latency to the
    // moment the boundary appears, widening the brief flash of the app underneath.
    private val overlayView: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_blocking_overlay, null)
    }

    private val overlayParams: WindowManager.LayoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Focusable (no FLAG_NOT_FOCUSABLE) so the buttons are tappable and the back key can't
            // slip past the boundary into the resting app.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    /**
     * Show the calm protected screen over a blocked app. It stays until the user makes a conscious
     * choice — [onUnblock] to write an intention and let the app through, or [onStepAway] to leave.
     */
    fun show(
        onUnblock: () -> Unit,
        onStepAway: () -> Unit,
    ) {
        if (!canDrawOverlay() || attached) return

        // Rewire the actions each time — the callbacks close over the current blocked package.
        overlayView.findViewById<TextView>(R.id.overlay_unblock_action)
            .setOnClickListener { onUnblock() }
        overlayView.findViewById<TextView>(R.id.overlay_step_away_action)
            .setOnClickListener { onStepAway() }

        runCatching { windowManager.addView(overlayView, overlayParams) }
            .onSuccess { attached = true }
    }

    fun hide() {
        if (!attached) return
        runCatching { windowManager.removeView(overlayView) }
        attached = false
    }
}
