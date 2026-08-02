package com.haoshield.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The journal's fade decides when a person's words start disappearing, so the curve is worth
 * proving rather than eyeballing.
 */
class JournalRetentionTest {

    private val day = 24L * 60 * 60 * 1_000

    @Test
    fun `fresh words are at full strength`() {
        assertEquals(1f, JournalRetention.alphaForAge(0), 0.0001f)
    }

    @Test
    fun `words stay at full strength right up to the fade`() {
        assertEquals(1f, JournalRetention.alphaForAge(14 * day), 0.0001f)
    }

    @Test
    fun `words are at their faintest by the time they are let go`() {
        assertEquals(
            JournalRetention.FADED_ALPHA,
            JournalRetention.alphaForAge(JournalRetention.RETAIN_MILLIS),
            0.0001f,
        )
    }

    @Test
    fun `the fade is monotonic - words never brighten with age`() {
        var previous = 1.1f
        for (days in 0..40) {
            val alpha = JournalRetention.alphaForAge(days * day)
            assertTrue("alpha rose at day $days", alpha <= previous + 0.0001f)
            previous = alpha
        }
    }

    @Test
    fun `halfway through the fade sits halfway down`() {
        val midpoint = (JournalRetention.FADE_BEGINS_MILLIS + JournalRetention.RETAIN_MILLIS) / 2
        val expected = (1f + JournalRetention.FADED_ALPHA) / 2f
        assertEquals(expected, JournalRetention.alphaForAge(midpoint), 0.01f)
    }

    @Test
    fun `words past their season are never invisible - they are readable until removed`() {
        // An entry can outlive its window briefly, between expiry and the next prune.
        assertTrue(JournalRetention.alphaForAge(90 * day) >= JournalRetention.FADED_ALPHA)
    }

    @Test
    fun `a clock that moves backwards does not brighten or break the fade`() {
        // Negative age means the device clock moved; it must clamp, not produce alpha above 1.
        assertEquals(1f, JournalRetention.alphaForAge(-day), 0.0001f)
    }
}
