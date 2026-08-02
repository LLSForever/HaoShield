package com.haoshield.domain.service

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The current moment, injected rather than reached for.
 *
 * The session rules are full of judgements about time — has this allowance run out, is this
 * restored session impossibly old, did the device clock move backwards, has enough passed to be
 * worth reflecting on. Every one of those is untestable while the code calls
 * `System.currentTimeMillis()` directly, and each is exactly the kind of rule that should be
 * proved rather than hoped over.
 */
fun interface Clock {
    fun nowMillis(): Long
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
