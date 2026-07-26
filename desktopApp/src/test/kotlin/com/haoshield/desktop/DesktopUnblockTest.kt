package com.haoshield.desktop

import com.haoshield.data.repository.BlockingRepositoryImpl
import com.haoshield.data.service.SessionManagerImpl
import com.haoshield.data.shield.ShieldTokenStoreImpl
import com.haoshield.desktop.blocking.ProcessName
import com.haoshield.desktop.data.DesktopBlockedAppsStore
import com.haoshield.desktop.data.DesktopJournalRepository
import com.haoshield.desktop.data.DesktopPreferences
import com.haoshield.desktop.data.DesktopSessionStore
import com.haoshield.desktop.data.DesktopShieldPreferences
import com.haoshield.desktop.data.NoStrictBlocking
import com.haoshield.desktop.data.WindowsBlockedAppPresets
import com.haoshield.domain.model.JournalEntryType
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.UnblockPolicy
import com.haoshield.domain.service.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Passing through the boundary. The rule that a note is required belongs to the domain; what is
 * desktop's own is that the watcher and the allowance agree on what an app is called.
 */
class DesktopUnblockTest {

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
        val blocking = BlockingRepositoryImpl(
            sessionManager = sessions,
            blockedAppsDataStore = DesktopBlockedAppsStore(preferences),
            presets = WindowsBlockedAppPresets,
        )
    }

    private fun withFixture(body: suspend (Fixture) -> Unit) = runTest {
        val fixture = Fixture(folder.root)
        try {
            fixture.sessions.awaitRestored()
            fixture.sessions.startSession(SessionMode.SOFTWARE)
            body(fixture)
        } finally {
            fixture.scope.cancel()
        }
    }

    private suspend fun Fixture.sessionId() = requireNotNull(sessions.getActiveSession()).id

    @Test
    fun `an app let through is one the watcher will leave alone`() = withFixture { fixture ->
        // The presets spell it "Discord.exe"; the watcher sees "discord.exe" from the process list.
        // If those two disagree, the app is closed a moment after being allowed.
        val asListed = "Discord.exe"

        val result = fixture.blocking.unblockAppForSession(
            sessionId = fixture.sessionId(),
            packageName = ProcessName.normalise(asListed),
            journalNote = "looking up one message",
        )

        assertTrue(result.isSuccess)
        assertTrue(
            "the watcher must recognise what was just allowed",
            fixture.sessions.isAppTemporarilyAllowed(ProcessName.normalise(asListed)),
        )
    }

    @Test
    fun `passing through costs a sentence`() = withFixture { fixture ->
        val result = fixture.blocking.unblockAppForSession(
            sessionId = fixture.sessionId(),
            packageName = "discord.exe",
            journalNote = "   ",
        )

        assertTrue("a blank reason is not a reason", result.isFailure)
        assertFalse(fixture.sessions.isAppTemporarilyAllowed("discord.exe"))
        assertTrue(
            "and nothing is written down for a refusal",
            fixture.journal.observeEntries().first().isEmpty(),
        )
    }

    @Test
    fun `the reason is kept, against the app it was given for`() = withFixture { fixture ->
        fixture.blocking.unblockAppForSession(
            sessionId = fixture.sessionId(),
            packageName = "steam.exe",
            journalNote = "checking whether the update finished",
        )

        val entry = fixture.journal.observeEntries().first().single()

        assertEquals(JournalEntryType.UNBLOCK, entry.type)
        assertEquals("steam.exe", entry.unblockedPackageName)
        assertEquals("checking whether the update finished", entry.content)
    }

    @Test
    fun `the boundary comes back`() = withFixture { fixture ->
        fixture.blocking.unblockAppForSession(
            sessionId = fixture.sessionId(),
            packageName = "discord.exe",
            journalNote = "one message",
        )
        assertTrue(fixture.sessions.isAppTemporarilyAllowed("discord.exe"))

        fixture.now += UnblockPolicy.UNBLOCK_WINDOW_MILLIS

        assertFalse(
            "passing through is a renewal, not a toll paid once",
            fixture.sessions.isAppTemporarilyAllowed("discord.exe"),
        )
    }

    @Test
    fun `letting one through does not open the rest`() = withFixture { fixture ->
        fixture.blocking.unblockAppForSession(
            sessionId = fixture.sessionId(),
            packageName = "discord.exe",
            journalNote = "one message",
        )

        assertFalse(fixture.sessions.isAppTemporarilyAllowed("steam.exe"))
    }

    @Test
    fun `an unblock meant for another session is refused`() = withFixture { fixture ->
        val result = fixture.blocking.unblockAppForSession(
            sessionId = fixture.sessionId() + 1,
            packageName = "discord.exe",
            journalNote = "one message",
        )

        assertTrue(result.isFailure)
        assertFalse(fixture.sessions.isAppTemporarilyAllowed("discord.exe"))
    }

    @Test
    fun `normalising is what the two sides agree on`() {
        assertEquals("discord.exe", ProcessName.normalise("Discord.exe"))
        assertEquals("discord.exe", ProcessName.normalise("  DISCORD.EXE  "))
        assertEquals(ProcessName.normalise("Steam.exe"), ProcessName.normalise("steam.EXE"))
    }
}
