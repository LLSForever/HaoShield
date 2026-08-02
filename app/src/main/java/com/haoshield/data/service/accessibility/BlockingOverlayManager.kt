package com.haoshield.data.service.accessibility

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.haoshield.R
import com.haoshield.data.local.SettingsPreferencesDataStore
import com.haoshield.domain.di.ApplicationScope
import com.haoshield.domain.model.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingOverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    settingsPreferencesDataStore: SettingsPreferencesDataStore,
    @ApplicationScope applicationScope: CoroutineScope,
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Whether the cached view is currently attached to the window manager.
    private var attached: Boolean = false

    // The overlay is inflated once and reused — re-inflating on every block added latency to the
    // moment the boundary appears, widening the flash of the app underneath. It is only rebuilt
    // when the resolved light/dark ground changes.
    private var overlayView: View? = null
    private var overlayIsDark: Boolean? = null

    // The user's appearance choice, mirrored here so show() stays synchronous.
    @Volatile private var themePreference: ThemePreference = ThemePreference.SYSTEM

    init {
        applicationScope.launch {
            settingsPreferencesDataStore.observeThemePreference().collect { preference ->
                themePreference = preference
            }
        }
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

    /** Whether the boundary is currently attached and covering the screen. */
    val isShowing: Boolean
        get() = attached

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    /**
     * Show the calm protected screen over a blocked app. It stays until the user makes a conscious
     * choice — [onUnblock] to write an intention and let the app through, or [onStepAway] to leave.
     */
    fun show(
        onUnblock: () -> Unit,
        onStepAway: () -> Unit,
        onOpenSession: () -> Unit,
        isRelock: Boolean = false,
    ) {
        if (!canDrawOverlay() || attached) return

        val view = viewFor(dark = resolveDark())

        // A re-lock (a temporary unblock that expired) reads differently from a first block — it
        // acknowledges the time that passed and invites renewal rather than just "resting".
        view.findViewById<TextView>(R.id.overlay_message).setText(
            if (isRelock) R.string.overlay_relock_message else R.string.overlay_protected_message,
        )

        // Rewire the actions each time — the callbacks close over the current blocked package.
        view.findViewById<TextView>(R.id.overlay_unblock_action)
            .setOnClickListener { onUnblock() }
        view.findViewById<TextView>(R.id.overlay_step_away_action)
            .setOnClickListener { onStepAway() }
        view.findViewById<TextView>(R.id.overlay_open_session_action)
            .setOnClickListener { onOpenSession() }

        runCatching { windowManager.addView(view, overlayParams) }
            .onSuccess { attached = true }
    }

    fun hide() {
        if (!attached) return
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        attached = false
    }

    /**
     * The overlay lives in its own window, so it does not inherit the app's in-process theme
     * choice — it would otherwise always follow the system. Inflating from a configuration-
     * overridden context makes values-night resolve to what the user actually picked.
     */
    private fun viewFor(dark: Boolean): View {
        overlayView?.let { cached ->
            if (overlayIsDark == dark) return cached
        }

        val configuration = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val themedContext = context.createConfigurationContext(configuration)
        val view = LayoutInflater.from(themedContext)
            .inflate(R.layout.view_blocking_overlay, null)

        overlayView = view
        overlayIsDark = dark
        return view
    }

    private fun resolveDark(): Boolean = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM ->
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }
}
