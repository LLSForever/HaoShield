package com.haoshield.domain.model

/**
 * How long an "unblock with intention" lasts. The boundary returns afterwards, so passing through
 * is a renewal of intention rather than a one-time toll that opens the app for the whole session.
 */
object UnblockPolicy {
    const val UNBLOCK_WINDOW_MILLIS: Long = 15 * 60 * 1_000L

    val windowMinutes: Long get() = UNBLOCK_WINDOW_MILLIS / 60_000L
}
