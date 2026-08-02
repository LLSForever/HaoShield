package com.haoshield.desktop

import com.haoshield.data.qr.ShieldQr
import com.haoshield.data.service.SessionManagerImpl
import com.haoshield.data.shield.ShieldScanHandlerImpl
import com.haoshield.data.shield.ShieldTokenStoreImpl
import com.haoshield.desktop.data.DesktopJournalRepository
import com.haoshield.desktop.data.DesktopPreferences
import com.haoshield.desktop.data.DesktopSessionStore
import com.haoshield.desktop.data.DesktopShieldPreferences
import com.haoshield.desktop.data.NoStrictBlocking
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.service.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The Shield on the desktop, end to end: making one, and the promise that a protected time cannot
 * be ended without it. The rules themselves are the phone's — this proves the desktop's wiring of
 * them is honest.
 */
class DesktopShieldFlowTest {

    @get:Rule
    val folder = TemporaryFolder()

    private class Fixture(directory: File) {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        private val preferences = DesktopPreferences(File(directory, "prefs.properties"))
        val tokens = ShieldTokenStoreImpl(DesktopShieldPreferences(preferences))
        val sessions = SessionManagerImpl(
            sessionPreferencesDataStore = DesktopSessionStore(preferences),
            journalRepository = DesktopJournalRepository(File(directory, "journal.tsv")),
            shieldTokenStore = tokens,
            strictBlockingController = NoStrictBlocking,
            clock = Clock { 1_700_000_000_000L },
            applicationScope = scope,
        )
        val handler = ShieldScanHandlerImpl(tokens, sessions)
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

    private fun qr(code: String) = ShieldToken(ShieldTokenKind.QR, code)

    @Test
    fun `a Shield written down and typed back becomes yours`() = withFixture { fixture ->
        val payload = ShieldQr.newPayload()
        fixture.tokens.setPendingQrPayload(payload)

        val result = fixture.handler.handleScan(qr(payload), ScanMode.REGISTRATION)

        assertTrue(result is ShieldScanResult.RegistrationComplete)
        assertTrue(fixture.tokens.hasAnyRegisteredToken())
    }

    @Test
    fun `a code that was never shown does not become a Shield`() = withFixture { fixture ->
        fixture.tokens.setPendingQrPayload(ShieldQr.newPayload())

        val result = fixture.handler.handleScan(
            qr(ShieldQr.PREFIX + "something-i-made-up"),
            ScanMode.REGISTRATION,
        )

        assertTrue("mistyping must not register a different Shield", result is ShieldScanResult.Failed)
        assertTrue(
            "and nothing should be registered at all",
            !fixture.tokens.hasAnyRegisteredToken(),
        )
    }

    @Test
    fun `a protected time does not end for the wrong code`() = withFixture { fixture ->
        val payload = register(fixture)
        fixture.sessions.startSession(SessionMode.SHIELD)

        val result = fixture.handler.handleScan(
            qr(ShieldQr.PREFIX + "8f14e45f-ceea-467a-9f4a-000000000000"),
            ScanMode.SESSION,
        )

        assertTrue(result is ShieldScanResult.InvalidShield)
        assertNotNull(
            "the session must still be standing after a wrong code",
            fixture.sessions.getActiveSession(),
        )
        // And the real one still works afterwards — a wrong guess doesn't lock you out.
        val ended = fixture.handler.handleScan(qr(payload), ScanMode.SESSION)
        assertTrue(ended is ShieldScanResult.SessionEnded)
        assertNull(fixture.sessions.getActiveSession())
    }

    @Test
    fun `a protected time ends when the Shield is presented`() = withFixture { fixture ->
        val payload = register(fixture)
        fixture.sessions.startSession(SessionMode.SHIELD)

        val result = fixture.handler.handleScan(qr(payload), ScanMode.SESSION)

        assertTrue(result is ShieldScanResult.SessionEnded)
        assertNull(fixture.sessions.getActiveSession())
    }

    @Test
    fun `presenting a Shield during an open session does not end it`() = withFixture { fixture ->
        val payload = register(fixture)
        fixture.sessions.startSession(SessionMode.SOFTWARE)

        val result = fixture.handler.handleScan(qr(payload), ScanMode.SESSION)

        assertEquals(ShieldScanResult.SoftwareSessionActive, result)
        assertNotNull(fixture.sessions.getActiveSession())
    }

    @Test
    fun `presenting a Shield before making one says so`() = withFixture { fixture ->
        val result = fixture.handler.handleScan(
            qr(ShieldQr.newPayload()),
            ScanMode.SESSION,
        )

        assertEquals(ShieldScanResult.NoShieldRegistered, result)
    }

    private suspend fun register(fixture: Fixture): String {
        val payload = ShieldQr.newPayload()
        fixture.tokens.setPendingQrPayload(payload)
        fixture.handler.handleScan(qr(payload), ScanMode.REGISTRATION)
        return payload
    }
}
