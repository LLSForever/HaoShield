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
    private var overlayView: View? = null

    val isShowing: Boolean
        get() = overlayView != null

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    /**
     * Show the calm protected screen over a blocked app. It stays until the user makes a conscious
     * choice — [onUnblock] to write an intention and let the app through, or [onStepAway] to leave.
     * If the screen is already showing, the actions are rebound so they target the latest app.
     */
    fun show(
        onUnblock: () -> Unit,
        onStepAway: () -> Unit,
    ) {
        if (!canDrawOverlay()) return

        overlayView?.let { view ->
            bindActions(view, onUnblock, onStepAway)
            return
        }

        val view = LayoutInflater.from(context).inflate(R.layout.view_blocking_overlay, null)
        bindActions(view, onUnblock, onStepAway)

        val params = WindowManager.LayoutParams(
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

        windowManager.addView(view, params)
        overlayView = view
    }

    private fun bindActions(
        view: View,
        onUnblock: () -> Unit,
        onStepAway: () -> Unit,
    ) {
        view.findViewById<TextView>(R.id.overlay_unblock_action).setOnClickListener { onUnblock() }
        view.findViewById<TextView>(R.id.overlay_step_away_action).setOnClickListener { onStepAway() }
    }

    fun hide() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }
}
