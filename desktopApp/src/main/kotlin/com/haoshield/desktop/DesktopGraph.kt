package com.haoshield.desktop

import com.haoshield.data.repository.BlockingRepositoryImpl
import com.haoshield.data.repository.SessionRepositoryImpl
import com.haoshield.data.repository.SettingsRepositoryImpl
import com.haoshield.data.service.SessionManagerImpl
import com.haoshield.data.shield.ShieldScanHandlerImpl
import com.haoshield.data.shield.ShieldTokenStoreImpl
import com.haoshield.desktop.blocking.WindowsAppBlocker
import com.haoshield.desktop.data.DesktopBlockedAppsStore
import com.haoshield.desktop.data.DesktopJournalRepository
import com.haoshield.desktop.data.DesktopPreferences
import com.haoshield.desktop.data.DesktopSessionStore
import com.haoshield.desktop.data.DesktopSettingsStore
import com.haoshield.desktop.data.DesktopShieldPreferences
import com.haoshield.desktop.data.NoStrictBlocking
import com.haoshield.desktop.data.WindowsBlockedAppPresets
import com.haoshield.domain.repository.BlockingRepository
import com.haoshield.domain.repository.JournalRepository
import com.haoshield.domain.repository.SessionRepository
import com.haoshield.domain.repository.SettingsRepository
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.service.ShieldScanHandler
import com.haoshield.domain.service.ShieldTokenStore
import com.haoshield.domain.service.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * The whole object graph, by hand.
 *
 * Hilt does this on Android; here it is a few dozen lines, which is the argument for having kept
 * the shared modules free of any DI framework. Everything below the line between platforms —
 * the session rules, the repositories, the Shield token handling — is the code Android runs.
 */
class DesktopGraph(directory: File = DesktopPreferences.appDirectory()) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val preferences = DesktopPreferences(File(directory, "preferences.properties"))

    // --- platform side ---

    private val sessionStore = DesktopSessionStore(preferences)
    private val settingsStore = DesktopSettingsStore(preferences)
    private val shieldPreferences = DesktopShieldPreferences(preferences)
    private val blockedAppsStore = DesktopBlockedAppsStore(preferences)

    val journalRepository: JournalRepository = DesktopJournalRepository(File(directory, "journal.tsv"))

    // --- shared with Android ---

    val shieldTokenStore: ShieldTokenStore = ShieldTokenStoreImpl(shieldPreferences)

    val sessionManager: SessionManager = SessionManagerImpl(
        sessionPreferencesDataStore = sessionStore,
        journalRepository = journalRepository,
        shieldTokenStore = shieldTokenStore,
        strictBlockingController = NoStrictBlocking,
        clock = SystemClock(),
        applicationScope = applicationScope,
    )

    val blockingRepository: BlockingRepository = BlockingRepositoryImpl(
        sessionManager = sessionManager,
        blockedAppsDataStore = blockedAppsStore,
        presets = WindowsBlockedAppPresets,
    )

    /**
     * The same handler the phone uses. It decides what a presented Shield means — register it,
     * start a session, or end one — so the desktop never re-implements that reasoning.
     */
    val shieldScanHandler: ShieldScanHandler = ShieldScanHandlerImpl(
        shieldTokenStore = shieldTokenStore,
        sessionManager = sessionManager,
    )

    val sessionRepository: SessionRepository = SessionRepositoryImpl(sessionManager)

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(settingsStore)

    // --- the blocker ---

    val blocker = WindowsAppBlocker(
        sessionManager = sessionManager,
        blockingRepository = blockingRepository,
        scope = applicationScope,
    )

    fun start() {
        blocker.start()
    }
}
