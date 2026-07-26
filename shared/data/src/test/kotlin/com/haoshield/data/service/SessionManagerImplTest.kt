package com.haoshield.data.service

import com.haoshield.domain.model.JournalEntryType
import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.model.UnblockPolicy
import com.haoshield.domain.service.SessionSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session rules — the load-bearing logic of the whole app.
 *
 * Two live bugs reached a device before these existed: a re-lock that cancelled itself, and a
 * strict-mode release that could strand apps suspended. Both were the kind of thing a test
 * catches in a millisecond and a person catches days later, if at all.
 *
 * No emulator and no Android here: every dependency is an interface, and time is injected.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerImplTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    private fun scenario(
        stored: SessionSnapshot? = null,
        registeredToken: ShieldToken? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val clock = TestClock()
        val store = FakeSessionStore(stored)
        val strict = FakeStrictBlocking()
        val journal = FakeJournalRepository()
        val tokens = FakeShieldTokenStore(registeredToken)
        // Unconfined so the manager's init-time restore runs eagerly.
        val applicationScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val manager = SessionManagerImpl(
            sessionPreferencesDataStore = store,
            journalRepository = journal,
            shieldTokenStore = tokens,
            strictBlockingController = strict,
            clock = clock,
            applicationScope = applicationScope,
        )
        manager.awaitRestored()
        try {
            Scenario(manager, clock, store, strict, journal).block()
        } finally {
            // The session's 1s timer parks on a delay and re-arms it for as long as the session
            // runs. Left alive on the test scheduler, runTest's end-of-test drain would advance
            // virtual time through those ticks forever, so any test leaving a session running
            // would hang. Cancelling first ends the loop.
            applicationScope.cancel()
        }
    }

    class Scenario(
        val manager: SessionManagerImpl,
        val clock: TestClock,
        val store: FakeSessionStore,
        val strict: FakeStrictBlocking,
        val journal: FakeJournalRepository,
    )

    private fun snapshotStartedAt(
        startedAt: Long,
        mode: SessionMode = SessionMode.SOFTWARE,
        allowances: Map<String, Long> = emptyMap(),
    ) = SessionSnapshot(
        session = Session(
            id = 1L,
            mode = mode,
            startedAtEpochMillis = startedAt,
            isActive = true,
        ),
        temporarilyAllowedPackages = allowances,
    )

    // --- Restoring a session the process left behind ------------------------------------------

    @Test
    fun `a recent session is restored`() = scenario(
        stored = snapshotStartedAt(TestClock().now - 10 * minute),
    ) {
        assertNotNull("a session from ten minutes ago should still be running", manager.getActiveSession())
    }

    @Test
    fun `a session older than any real sitting is let go, not resurrected`() = scenario(
        stored = snapshotStartedAt(TestClock().now - 40 * day),
    ) {
        // This is the bug that showed a clock reading 953 hours.
        assertNull(manager.getActiveSession())
        assertTrue("the impossible snapshot should be cleared", store.clearCount > 0)
    }

    @Test
    fun `a session starting in the future means the clock moved, so it is let go`() = scenario(
        stored = snapshotStartedAt(TestClock().now + 2 * hour),
    ) {
        assertNull(manager.getActiveSession())
    }

    @Test
    fun `a small backwards clock nudge does not throw away a live session`() = scenario(
        // Within tolerance: a minute of drift or an NTP correction shouldn't end someone's session.
        stored = snapshotStartedAt(TestClock().now + minute),
    ) {
        assertNotNull(manager.getActiveSession())
    }

    @Test
    fun `allowances that ran out while the process was dead do not survive the restore`() {
        val now = TestClock().now
        scenario(
            stored = snapshotStartedAt(
                startedAt = now - 10 * minute,
                allowances = mapOf(
                    "com.expired" to now - minute,
                    "com.still.open" to now + 5 * minute,
                ),
            ),
        ) {
            assertFalse(manager.isAppTemporarilyAllowed("com.expired"))
            assertTrue(manager.isAppTemporarilyAllowed("com.still.open"))
        }
    }

    @Test
    fun `a process that died mid-session releases any strict suspensions on restore`() = scenario() {
        // No stored session: whatever strict mode had suspended must be let go, or apps stay
        // paused with no session left to end them.
        assertTrue(strict.releaseAllCount > 0)
    }

    // --- Temporary unblocks -------------------------------------------------------------------

    @Test
    fun `an unblock lasts exactly its window and not a moment longer`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        manager.allowAppTemporarily("com.instagram.android", "just checking one thing")

        assertTrue(manager.isAppTemporarilyAllowed("com.instagram.android"))

        clock.advanceBy(UnblockPolicy.UNBLOCK_WINDOW_MILLIS - 1)
        assertTrue("still inside the window", manager.isAppTemporarilyAllowed("com.instagram.android"))

        clock.advanceBy(2)
        assertFalse("the boundary should return", manager.isAppTemporarilyAllowed("com.instagram.android"))
    }

    @Test
    fun `unblocking writes the intention to the journal against its session`() = scenario {
        val session = manager.startSession(SessionMode.SOFTWARE)
        manager.allowAppTemporarily("com.instagram.android", "replying to my sister")

        val entry = journal.saved.single()
        assertEquals("replying to my sister", entry.content)
        assertEquals(JournalEntryType.UNBLOCK, entry.type)
        assertEquals("com.instagram.android", entry.unblockedPackageName)
        assertEquals(session.id, entry.sessionId)
    }

    @Test
    fun `unblocking asks strict mode to re-suspend when the allowance runs out`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        manager.allowAppTemporarily("com.instagram.android", "a note")

        assertEquals(
            "com.instagram.android" to UnblockPolicy.UNBLOCK_WINDOW_MILLIS,
            strict.releasedPackages.single(),
        )
    }

    @Test
    fun `an unblock with no note is refused`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        val result = manager.allowAppTemporarily("com.instagram.android", "   ")

        assertTrue(result.isFailure)
        assertTrue("nothing should be written", journal.saved.isEmpty())
    }

    @Test
    fun `if strict mode cannot release the app, nothing is written and nothing is allowed`() =
        scenario {
            manager.startSession(SessionMode.SOFTWARE)
            strict.failOnRelease = true

            val result = manager.allowAppTemporarily("com.instagram.android", "a note")

            // The ordering that matters: a failed release must not leave a journal entry behind
            // to be duplicated on retry, nor mark an app allowed that is still OS-suspended.
            assertTrue(result.isFailure)
            assertTrue(journal.saved.isEmpty())
            assertFalse(manager.isAppTemporarilyAllowed("com.instagram.android"))
        }

    // --- Ending a session ---------------------------------------------------------------------

    @Test
    fun `a Software session ends from within the app`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        val result = manager.endSession(SessionEndMethod.IN_APP)

        assertTrue(result is SessionEndResult.Ended)
        assertNull(manager.getActiveSession())
    }

    @Test
    fun `a Shield session asks for the Shield rather than ending on a tap`() = scenario(
        registeredToken = ShieldToken(ShieldTokenKind.NFC, "ABC"),
    ) {
        manager.startSession(SessionMode.SHIELD)
        val result = manager.endSession(SessionEndMethod.IN_APP)

        assertTrue(result is SessionEndResult.RequiresShieldScan)
        assertNotNull("the session must still be running", manager.getActiveSession())
    }

    @Test
    fun `a Shield session ends for the registered Shield`() = scenario(
        registeredToken = ShieldToken(ShieldTokenKind.NFC, "ABC"),
    ) {
        manager.startSession(SessionMode.SHIELD)
        val result = manager.endSession(
            method = SessionEndMethod.SHIELD_SCAN,
            shieldToken = ShieldToken(ShieldTokenKind.NFC, "abc"), // read in lower case
        )

        assertTrue(result is SessionEndResult.Ended)
    }

    @Test
    fun `someone else's Shield does not end your session`() = scenario(
        registeredToken = ShieldToken(ShieldTokenKind.NFC, "ABC"),
    ) {
        manager.startSession(SessionMode.SHIELD)
        val result = manager.endSession(
            method = SessionEndMethod.SHIELD_SCAN,
            shieldToken = ShieldToken(ShieldTokenKind.NFC, "DIFFERENT"),
        )

        assertTrue(result is SessionEndResult.Failed)
        assertNotNull(manager.getActiveSession())
    }

    @Test
    fun `ending releases strict suspensions so apps are usable again`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        val before = strict.releaseAllCount
        manager.endSession(SessionEndMethod.IN_APP)

        assertTrue(strict.releaseAllCount > before)
    }

    // --- Reflection: offered when it means something, not every time ---------------------------

    @Test
    fun `a session you named an intention for is worth reflecting on`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        manager.setSessionIntention("reading with my daughter")
        clock.advanceBy(2 * minute)
        manager.endSession(SessionEndMethod.IN_APP)

        val summary = manager.getLastEndedSession()
        assertNotNull(summary)
        assertEquals("reading with my daughter", summary?.intention)
    }

    @Test
    fun `a brief unnamed session ends without asking anything of you`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        clock.advanceBy(2 * minute)
        manager.endSession(SessionEndMethod.IN_APP)

        assertNull("a two-minute session shouldn't prompt", manager.getLastEndedSession())
    }

    @Test
    fun `a long session earns a reflection even with no intention named`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        clock.advanceBy(45 * minute)
        manager.endSession(SessionEndMethod.IN_APP)

        val summary = manager.getLastEndedSession()
        assertNotNull(summary)
        assertNull(summary?.intention)
        assertEquals(45 * minute, summary?.durationMillis)
    }

    @Test
    fun `an emergency exit does not then ask you to reflect - the note was the reflection`() =
        scenario(registeredToken = ShieldToken(ShieldTokenKind.NFC, "ABC")) {
            manager.startSession(SessionMode.SHIELD)
            manager.setSessionIntention("deep work")
            clock.advanceBy(30 * minute)

            val result = manager.endShieldSessionByEmergency("I have to take a call")

            assertTrue(result is SessionEndResult.Ended)
            assertNull(manager.getLastEndedSession())
            assertEquals(
                JournalEntryType.EMERGENCY_EXIT,
                journal.saved.single { it.type == JournalEntryType.EMERGENCY_EXIT }.type,
            )
        }

    @Test
    fun `an emergency exit needs words`() = scenario(
        registeredToken = ShieldToken(ShieldTokenKind.NFC, "ABC"),
    ) {
        manager.startSession(SessionMode.SHIELD)
        val result = manager.endShieldSessionByEmergency("  ")

        assertTrue(result is SessionEndResult.Failed)
        assertNotNull(manager.getActiveSession())
    }

    @Test
    fun `starting a new session clears any reflection left pending from the last`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        manager.setSessionIntention("first")
        clock.advanceBy(30 * minute)
        manager.endSession(SessionEndMethod.IN_APP)
        assertNotNull(manager.getLastEndedSession())

        manager.startSession(SessionMode.SOFTWARE)

        assertNull("a new session must not surface the old one's prompt", manager.getLastEndedSession())
    }

    // --- Intention ----------------------------------------------------------------------------

    @Test
    fun `an intention is kept, trimmed, and written down`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        manager.setSessionIntention("   time with family   ")

        assertEquals("time with family", manager.getActiveSession()?.intention)
        assertEquals("time with family", store.current?.session?.intention)
    }

    @Test
    fun `an intention cannot run away with the screen`() = scenario {
        manager.startSession(SessionMode.SOFTWARE)
        manager.setSessionIntention("x".repeat(500))

        val kept = manager.getActiveSession()?.intention.orEmpty()
        assertTrue("an intention should stay a sentence, not an essay", kept.length <= 120)
    }

    @Test
    fun `naming an intention with no session running does nothing`() = scenario {
        manager.setSessionIntention("nothing to attach this to")
        assertNull(manager.getActiveSession())
    }

    // --- Starting -----------------------------------------------------------------------------

    @Test
    fun `starting a session suspends the blocked apps and writes the session down`() = scenario {
        val session = manager.startSession(SessionMode.SOFTWARE)

        assertEquals(1, strict.applyCount)
        assertEquals(session.id, store.current?.session?.id)
        assertEquals(clock.now, session.startedAtEpochMillis)
    }

    @Test
    fun `starting replaces a running session rather than stacking one on top`() = scenario {
        val first = manager.startSession(SessionMode.SOFTWARE)
        val second = manager.startSession(SessionMode.SHIELD)

        assertTrue(first.id != second.id)
        assertEquals(SessionMode.SHIELD, manager.getActiveSession()?.mode)
    }
}
