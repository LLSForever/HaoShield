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
            "com.instagram.barcelona",    // Threads
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
)

/**
 * Human names for the curated packages.
 *
 * PackageManager can only name an app that is *installed*, so anything on the preset list the
 * person doesn't have would otherwise show as a raw id ("com.zhiliaoapp.musically"). These names
 * let the blocked-apps list stay readable either way.
 */
internal val PresetAppNames: Map<String, String> = mapOf(
    "com.instagram.android" to "Instagram",
    "com.zhiliaoapp.musically" to "TikTok",
    "com.facebook.katana" to "Facebook",
    "com.twitter.android" to "X",
    "com.snapchat.android" to "Snapchat",
    "com.reddit.frontpage" to "Reddit",
    "com.pinterest" to "Pinterest",
    "com.instagram.barcelona" to "Threads",
    "com.google.android.youtube" to "YouTube",
    "com.netflix.mediaclient" to "Netflix",
    "tv.twitch.android.app" to "Twitch",
)
