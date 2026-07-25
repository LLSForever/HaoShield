package com.haoshield.domain.model

/**
 * The journal keeps a season, not an archive.
 *
 * Entries are written at moments of friction — passing a boundary, ending early, closing a
 * session — and their worth is in the writing, not in accumulating. Left to grow, the journal
 * becomes a ledger to scroll and manage, which is the hoarding logic this app exists to resist.
 *
 * So entries dim as they age and are let go after [RETAIN_MILLIS]. The fade is the point: words
 * visibly pass rather than silently disappearing overnight.
 */
object JournalRetention {
    const val RETAIN_MILLIS: Long = 30L * 24 * 60 * 60 * 1_000

    /** Entries are full-strength until this age, then dim towards the end of their season. */
    const val FADE_BEGINS_MILLIS: Long = 14L * 24 * 60 * 60 * 1_000

    /** How faint an entry gets just before it goes. Never invisible — it is still readable. */
    const val FADED_ALPHA: Float = 0.4f

    /** 1.0 when fresh, easing to [FADED_ALPHA] as the entry approaches its last days. */
    fun alphaForAge(ageMillis: Long): Float = when {
        ageMillis <= FADE_BEGINS_MILLIS -> 1f
        ageMillis >= RETAIN_MILLIS -> FADED_ALPHA
        else -> {
            val progress = (ageMillis - FADE_BEGINS_MILLIS).toFloat() /
                (RETAIN_MILLIS - FADE_BEGINS_MILLIS).toFloat()
            1f - progress * (1f - FADED_ALPHA)
        }
    }
}
