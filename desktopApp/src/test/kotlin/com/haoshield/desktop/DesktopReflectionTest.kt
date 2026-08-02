package com.haoshield.desktop

import com.haoshield.data.service.SessionManagerImpl
import com.haoshield.data.shield.ShieldTokenStoreImpl
import com.haoshield.desktop.data.DesktopJournalRepository
import com.haoshield.desktop.data.DesktopPreferences
import com.haoshield.desktop.data.DesktopSessionStore
import com.haoshield.desktop.data.DesktopShieldPreferences
import com.haoshield.desktop.data.NoStrictBlocking
import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalEntryType
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.service.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Whether a time is worth pausing over is the session rules' judgement, not the desktop's. These
 * check that the desktop asks at the right moments and keeps what it is given.
 */
class DesktopReflectionTest {

    @get:Rule
    val folder = TemporaryFolder()

    private class Fixture(directory: File) {
        var now: Long = 1_700_000_000_000L
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val journal = DesktopJournalRepository(File(directory, "journal.tsv"))
        private val preferences = DesktopPreferences(File(directory, "prefs.properties"))
        val sessions = SessionManagerImpl(
            sessionPreferencesDataStore = DesktopSessionStore(preferences),
            journalRepository = journal,
            shieldTokenStore = ShieldTokenStoreImpl(DesktopShieldPreferences(preferences)),
            strictBlockingController = NoStrictBlocking,
            clock = Clock { now },
            applicationScope = scope,
        )
    }

    private fun withFixture(body: suspend (Fixture) -> Unit) = runTest {
        val fixture = Fixture(folder.root)
        try {
            fixture.sessions.awaitRestored()
            body(fixture)
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun `a time you named an intention for is worth pausing over`() = withFixture { fixture ->
        fixture.sessions.startSession(SessionMode.SOFTWARE)
        fixture.sessions.setSessionIntention("finish the chapter")
        fixture.sessions.endSession(SessionEndMethod.IN_APP)

        val summary = fixture.sessions.getLastEndedSession()

        assertNotNull("an intention deserves closing", summary)
        assertEquals("finish the chapter", summary?.intention)
    }

    @Test
    fun `naming a time keeps it, across the app closing`() = withFixture { fixture ->
        fixture.sessions.startSession(SessionMode.SOFTWARE)

        fixture.sessions.setSessionIntention("write the difficult chapter")

        assertEquals(
            "write the difficult chapter",
            fixture.sessions.getActiveSession()?.intention,
        )
    }

    @Test
    fun `a name makes even a brief time worth pausing over`() = withFixture { fixture ->
        fixture.sessions.startSession(SessionMode.SOFTWARE)
        fixture.sessions.setSessionIntention("one small thing")
        fixture.now += 30_000L
        fixture.sessions.endSession(SessionEndMethod.IN_APP)

        assertNotNull(
            "the same half-minute goes unremarked without a name, and is worth closing with one",
            fixture.sessions.getLastEndedSession(),
        )
    }

    @Test
    fun `a long time is worth pausing over even with nothing named`() = withFixture { fixture ->
        fixture.sessions.startSession(SessionMode.SOFTWARE)
        fixture.now += 45 * 60 * 1_000L
        fixture.sessions.endSession(SessionEndMethod.IN_APP)

        assertNotNull(fixture.sessions.getLastEndedSession())
    }

    @Test
    fun `a brief, unnamed sitting is not made into a ceremony`() = withFixture { fixture ->
        fixture.sessions.startSession(SessionMode.SOFTWARE)
        fixture.now += 30_000L
        fixture.sessions.endSession(SessionEndMethod.IN_APP)

        assertNull(
            "nothing was set aside and nothing lasted — do not ask",
            fixture.sessions.getLastEndedSession(),
        )
    }

    @Test
    fun `a reflection is kept, and the asking stops`() = withFixture { fixture ->
        fixture.sessions.startSession(SessionMode.SOFTWARE)
        fixture.sessions.setSessionIntention("sit with it")
        fixture.sessions.endSession(SessionEndMethod.IN_APP)
        val summary = requireNotNull(fixture.sessions.getLastEndedSession())

        fixture.journal.saveEntry(
            JournalEntry(
                content = "Easier than I expected.",
                createdAtEpochMillis = fixture.now,
                sessionId = summary.sessionId,
                type = JournalEntryType.REFLECTION,
            ),
        )
        fixture.sessions.clearLastEndedSession()

        val kept = fixture.journal.observeEntries().first()
        assertEquals(1, kept.size)
        assertEquals("Easier than I expected.", kept.single().content)
        assertEquals(JournalEntryType.REFLECTION, kept.single().type)
        assertEquals(summary.sessionId, kept.single().sessionId)
        assertNull("having answered once, it must not ask again", fixture.sessions.getLastEndedSession())
    }

    @Test
    fun `declining is a real answer and is not asked twice`() = withFixture { fixture ->
        fixture.sessions.startSession(SessionMode.SOFTWARE)
        fixture.sessions.setSessionIntention("rest")
        fixture.sessions.endSession(SessionEndMethod.IN_APP)
        assertNotNull(fixture.sessions.getLastEndedSession())

        fixture.sessions.clearLastEndedSession()

        assertNull(fixture.sessions.getLastEndedSession())
        assertEquals(
            "declining writes nothing down",
            emptyList<JournalEntry>(),
            fixture.journal.observeEntries().first(),
        )
    }
}
