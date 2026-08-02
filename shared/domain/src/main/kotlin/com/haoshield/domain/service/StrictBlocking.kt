package com.haoshield.domain.service

/**
 * Genuine OS-level suspension of blocked apps, where the platform allows it.
 *
 * The session rules call this at the boundaries of a session without knowing — or needing to know
 * — that on Android it means `pm suspend` over a root shell, and that on most devices it does
 * nothing at all. Every method is a no-op when strict blocking is unavailable or switched off.
 */
interface StrictBlocking {
    /** Suspend the blocked apps for a session that is starting. */
    suspend fun applyForSession()

    /** Release everything suspended. Safe to call when nothing is. */
    suspend fun releaseAll()

    /**
     * Release one app so it can be opened, optionally re-suspending it once its allowance runs
     * out.
     */
    suspend fun release(packageName: String, resuspendAfterMillis: Long? = null)
}
