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

    data object Protected : Route("protected")

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