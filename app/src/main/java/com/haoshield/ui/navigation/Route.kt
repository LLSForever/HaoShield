package com.haoshield.ui.navigation

import android.net.Uri
import com.haoshield.domain.model.ScanMode

sealed class Route(val path: String) {
    data object Intro : Route("intro")

    data object GettingStarted : Route("getting_started")

    data object Home : Route("home")

    data object Setup : Route("setup")

    data object Settings : Route("settings")

    data object Permissions : Route("permissions")

    /**
     * The running session. The optional argument opens the without-your-Shield flow on arrival,
     * so Settings can reach it without a second copy of that panel existing.
     */
    data object Protected : Route("protected?emergency={emergency}") {
        const val ARG_EMERGENCY = "emergency"

        fun createRoute(emergency: Boolean = false): String = "protected?emergency=$emergency"
    }

    data object Reflection : Route("reflection")

    data object Journal : Route("journal")

    data object Unblock : Route("unblock/{packageName}") {
        fun createRoute(packageName: String): String =
            "unblock/${Uri.encode(packageName)}"
    }

    data object BlockedApps : Route("blocked_apps")

    data object AppPicker : Route("app_picker")

    data object Guide : Route("guide")

    data object QrScanner : Route("qr_scanner/{scanMode}") {
        const val ARG_SCAN_MODE = "scanMode"

        fun createRoute(mode: ScanMode): String = "qr_scanner/${mode.name}"
    }
}