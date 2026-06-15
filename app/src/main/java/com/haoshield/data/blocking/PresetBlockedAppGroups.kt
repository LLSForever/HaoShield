package com.haoshield.data.blocking

import com.haoshield.domain.model.BlockedAppGroup

/**
 * Curated Phase 1 preset groups. These define the apps a protected session shields you from.
 *
 * Any app here is blocked for the duration of a session unless you consciously unblock it with a
 * journal note — the gentle boundary, not a wall. Package names are stable, well-known ids; edit
 * this list to refine the curated set.
 */
internal val PresetBlockedAppGroups: List<BlockedAppGroup> = listOf(
    BlockedAppGroup(
        id = "social",
        displayName = "Social media",
        packageNames = listOf(
            "com.instagram.android",      // Instagram
            "com.zhiliaoapp.musically",   // TikTok
            "com.facebook.katana",        // Facebook
            "com.twitter.android",        // X (Twitter)
            "com.snapchat.android",       // Snapchat
            "com.reddit.frontpage",       // Reddit
            "com.pinterest",              // Pinterest
        ),
    ),
    BlockedAppGroup(
        id = "video",
        displayName = "Video & streaming",
        packageNames = listOf(
            "com.google.android.youtube", // YouTube
            "com.netflix.mediaclient",    // Netflix
            "tv.twitch.android.app",      // Twitch
        ),
    ),
    BlockedAppGroup(
        id = "messaging",
        displayName = "Messaging",
        packageNames = listOf(
            "com.whatsapp",               // WhatsApp
            "com.facebook.orca",          // Messenger
            "org.telegram.messenger",     // Telegram
            "com.discord",                // Discord
        ),
    ),
)
