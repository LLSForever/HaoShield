package com.haoshield.desktop.data

import com.haoshield.domain.model.BlockedAppGroup
import com.haoshield.domain.service.BlockedAppPresets

/**
 * The curated Windows list — executable names rather than Android package names, which is exactly
 * why [BlockedAppPresets] is an interface.
 *
 * Names are matched case-insensitively against the executable of each running process. Browsers are
 * deliberately absent: closing someone's browser mid-session is more likely to destroy work than to
 * protect attention, and the honest answer for sites is a browser extension rather than a killed
 * window.
 */
object WindowsBlockedAppPresets : BlockedAppPresets {

    override val groups: List<BlockedAppGroup> = listOf(
        BlockedAppGroup(
            id = "social",
            displayName = "Social and messaging",
            packageNames = listOf(
                "Discord.exe",
                "Telegram.exe",
                "WhatsApp.exe",
                "Signal.exe",
                "Slack.exe",
            ),
        ),
        BlockedAppGroup(
            id = "games",
            displayName = "Games and launchers",
            packageNames = listOf(
                "steam.exe",
                "EpicGamesLauncher.exe",
                "Battle.net.exe",
                "GalaxyClient.exe",
                "RobloxPlayerBeta.exe",
                "Minecraft.exe",
            ),
        ),
        BlockedAppGroup(
            id = "video",
            displayName = "Video and streaming",
            packageNames = listOf(
                "Netflix.exe",
                "Spotify.exe",
                "vlc.exe",
            ),
        ),
    )
}
