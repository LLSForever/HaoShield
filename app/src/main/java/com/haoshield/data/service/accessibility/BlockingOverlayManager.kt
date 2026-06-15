package com.haoshield.data.service.accessibility

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
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
    private var overlayView: android.view.View? = null

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun show() {
        if (!canDrawOverlay() || overlayView != null) return

        val view = LayoutInflater.from(context).inflate(R.layout.view_blocking_overlay, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(view, params)
        overlayView = view
    }

    fun hide() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }
}