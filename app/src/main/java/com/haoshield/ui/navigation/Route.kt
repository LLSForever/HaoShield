package com.haoshield.ui.navigation

import android.net.Uri

sealed class Route(val path: String) {
    data object Home : Route("home")

    data object SoftwareMode : Route("software_mode")

    data object ShieldMode : Route("shield_mode")

    data object Protected : Route("protected")

    data object Journal : Route("journal")

    data object Unblock : Route("unblock/{packageName}") {
        fun createRoute(packageName: String): String =
            "unblock/${Uri.encode(packageName)}"
    }

    data object Guide : Route("guide")

    data object Letters : Route("letters")
}