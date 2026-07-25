package com.haoshield.data.service

import com.haoshield.data.blocking.StrictBlockingController
import com.haoshield.data.local.PersistedSessionSnapshot
import com.haoshield.data.local.SessionPreferencesDataStore
import com.haoshield.di.ApplicationScope
import com.haoshield.domain.model.EndedSessionSummary
import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalEntryType
import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.SessionState
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.UnblockPolicy
import com.haoshield.domain.repository.JournalRepository
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.service.ShieldTokenStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManagerImpl @Inject constructor(
    private val sessionPreferencesDataStore: SessionPreferencesDataStore,
    private val journalRepository: JournalRepository,
    private val shieldTokenStore: ShieldTokenStore,
    private val strictBlockingController: StrictBlockingController,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : SessionManager {

    private val sessionState = MutableStateFlow<SessionState?>(null)
    private var timerJob: Job? = null

    // The most recently ended session, held for the reflection screen to consume. Null for
    // emergency exits (which already captured a note).
    @Volatile private var lastEndedSession: EndedSessionSummary? = null

    // Completed once the persisted session has been restored after process start. Blocking checks
    // await this so a blocked app opened during the restore window isn't briefly let through.
    private val restored = CompletableDeferred<Unit>()

    init {
        applicationScope.launch {
            restorePersistedSession()
        }
    }

    override suspend fun awaitRestored() {
        restored.await()
    }

    override fun observeSessionState(): Flow<SessionState?> = sessionState.asStateFlow()

    override suspend fun getActiveSession(): Session? = sessionState.value?.session

    override suspend fun startSession(mode: SessionMode): Session {
        // Replacing any prior session doesn't offer a reflection, and a fresh start clears any
        // pending one so a new session's end doesn't surface a stale prompt.
        clearActiveSessionForReplacement(recordForReflection = false)
        lastEndedSession = null

        val session = Session(
            id = UUID.randomUUID().mostSignificantBits,
            mode = mode,
            startedAtEpochMillis = System.currentTimeMillis(),
            isActive = true,
        )

        val state = SessionState(
            session = session,
            elapsedMillis = 0L,
            temporarilyAllowedPackages = emptyMap(),
        )

        sessionState.value = state
        sessionPreferencesDataStore.persistActiveSession(
            session = session,
            temporarilyAllowedPackages = emptyMap(),
        )
        startTimer(session.startedAtEpochMillis)

        // Strict mode (rooted devices): genuinely suspend the blocked apps for this session.
        // No-op otherwise. Runs after clearActiveSessionForReplacement() above has released any
        // leftover suspensions, so ordering is deterministic.
        strictBlockingController.applyForSession()

        return session
    }

    override suspend fun endSession(
        method: SessionEndMethod,
        shieldToken: ShieldToken?,
    ): SessionEndResult {
        val current = sessionState.value ?: return SessionEndResult.Failed("No active session.")

        return when (current.session.mode) {
            SessionMode.SOFTWARE -> endSoftwareSession(method)
            SessionMode.SHIELD -> endShieldSession(method, shieldToken)
        }
    }

    override suspend fun allowAppTemporarily(
        packageName: String,
        journalNote: String,
    ): Result<Unit> = runCatching {
        val current = sessionState.value
            ?: throw IllegalStateException("No active session.")
        if (journalNote.isBlank()) {
            throw IllegalArgumentException("Journal note is required to unblock an app.")
        }

        // In strict mode the app is OS-suspended — release it FIRST so it can actually open, and so
        // a root failure aborts here (throwing) before we mutate state or write a journal entry.
        // Otherwise a retry after a failed release would duplicate the journal note. The window
        // matches the allowance, after which strict mode re-suspends the app.
        strictBlockingController.release(
            packageName = packageName,
            resuspendAfterMillis = UnblockPolicy.UNBLOCK_WINDOW_MILLIS,
        )

        journalRepository.saveEntry(
            JournalEntry(
                content = journalNote,
                createdAtEpochMillis = System.currentTimeMillis(),
                sessionId = current.session.id,
                type = JournalEntryType.UNBLOCK,
                unblockedPackageName = packageName,
            ),
        )

        val allowedUntil = System.currentTimeMillis() + UnblockPolicy.UNBLOCK_WINDOW_MILLIS
        val updatedPackages = current.temporarilyAllowedPackages + (packageName to allowedUntil)
        sessionState.value = current.copy(temporarilyAllowedPackages = updatedPackages)
        sessionPreferencesDataStore.persistAllowedPackages(updatedPackages)
    }

    override suspend fun setSessionIntention(intention: String) {
        val current = sessionState.value ?: return
        val trimmed = intention.trim().take(INTENTION_MAX_LENGTH)
        sessionState.value = current.copy(session = current.session.copy(intention = trimmed))
        sessionPreferencesDataStore.persistIntention(trimmed)
    }

    override fun getLastEndedSession(): EndedSessionSummary? = lastEndedSession

    override fun clearLastEndedSession() {
        lastEndedSession = null
    }

    override suspend fun isAppTemporarilyAllowed(packageName: String): Boolean {
        val expiry = sessionState.value?.temporarilyAllowedPackages?.get(packageName) ?: return false
        return System.currentTimeMillis() < expiry
    }

    override suspend fun getTemporaryAllowanceExpiry(packageName: String): Long? {
        val expiry = sessionState.value?.temporarilyAllowedPackages?.get(packageName) ?: return null
        return expiry.takeIf { System.currentTimeMillis() < it }
    }

    override suspend fun restorePersistedSession() {
        try {
            val snapshot = sessionPreferencesDataStore.observePersistedSession().first()
            restoreFromSnapshot(snapshot)
        } finally {
            // Unblock awaiters even if restore fails, so blocking never deadlocks on a bad read.
            if (!restored.isCompleted) restored.complete(Unit)
        }
    }

    private suspend fun restoreFromSnapshot(snapshot: PersistedSessionSnapshot?) {
        if (snapshot == null) {
            sessionState.value = null
            stopTimer()
            // If a strict session was suspended when the process was killed, release it now so the
            // user isn't left with apps stuck suspended and no session to end.
            strictBlockingController.releaseAll()
            return
        }

        val elapsed = System.currentTimeMillis() - snapshot.session.startedAtEpochMillis
        val now = System.currentTimeMillis()
        sessionState.value = SessionState(
            session = snapshot.session,
            elapsedMillis = elapsed.coerceAtLeast(0L),
            // Drop allowances that expired while the process was dead — they re-block on restore.
            temporarilyAllowedPackages = snapshot.temporarilyAllowedPackages
                .filterValues { it > now },
        )
        startTimer(snapshot.session.startedAtEpochMillis)
    }

    private suspend fun endSoftwareSession(method: SessionEndMethod): SessionEndResult {
        if (method != SessionEndMethod.IN_APP) {
            return SessionEndResult.Failed("Software sessions end from within the app.")
        }
        return clearActiveSessionForReplacement()
            ?.let { SessionEndResult.Ended(it) }
            ?: SessionEndResult.Failed("Unable to end session.")
    }

    private suspend fun endShieldSession(
        method: SessionEndMethod,
        shieldToken: ShieldToken?,
    ): SessionEndResult {
        return when (method) {
            SessionEndMethod.IN_APP -> SessionEndResult.RequiresShieldScan()
            SessionEndMethod.SHIELD_SCAN -> {
                val token = shieldToken?.takeIf { it.id.isNotBlank() }
                    ?: return SessionEndResult.Failed("Shield token is required.")
                if (!shieldTokenStore.validate(token)) {
                    return SessionEndResult.Failed("This is not your registered Hǎo Shield.")
                }
                clearActiveSessionForReplacement()
                    ?.let { SessionEndResult.Ended(it) }
                    ?: SessionEndResult.Failed("Unable to end session.")
            }
            SessionEndMethod.EMERGENCY ->
                SessionEndResult.Failed("Use the emergency exit to end without your Shield.")
        }
    }

    override suspend fun endShieldSessionByEmergency(note: String): SessionEndResult {
        val current = sessionState.value
            ?: return SessionEndResult.Failed("No active session.")
        if (current.session.mode != SessionMode.SHIELD) {
            return SessionEndResult.Failed("Only Shield sessions use the emergency exit.")
        }
        if (note.isBlank()) {
            return SessionEndResult.Failed("An intention note is required.")
        }

        journalRepository.saveEntry(
            JournalEntry(
                content = note,
                createdAtEpochMillis = System.currentTimeMillis(),
                sessionId = current.session.id,
                type = JournalEntryType.EMERGENCY_EXIT,
            ),
        )

        // The emergency note is the reflection — don't prompt again.
        return clearActiveSessionForReplacement(recordForReflection = false)
            ?.let { SessionEndResult.Ended(it) }
            ?: SessionEndResult.Failed("Unable to end session.")
    }

    private suspend fun clearActiveSessionForReplacement(
        recordForReflection: Boolean = true,
    ): Session? {
        val current = sessionState.value ?: return null

        val now = System.currentTimeMillis()
        val endedSession = current.session.copy(
            endedAtEpochMillis = now,
            isActive = false,
        )

        // A normal end offers a reflection; a start-replacement or emergency exit does not.
        lastEndedSession = if (recordForReflection) {
            EndedSessionSummary(
                sessionId = current.session.id,
                mode = current.session.mode,
                durationMillis = (now - current.session.startedAtEpochMillis).coerceAtLeast(0L),
                intention = current.session.intention,
            )
        } else {
            null
        }

        sessionState.value = null
        stopTimer()
        sessionPreferencesDataStore.clearSession()

        // Release any strict-mode suspensions so the apps are usable again once the session ends.
        strictBlockingController.releaseAll()

        return endedSession
    }

    private fun startTimer(startedAtEpochMillis: Long) {
        stopTimer()
        timerJob = applicationScope.launch {
            while (true) {
                val current = sessionState.value ?: break
                val elapsed = (System.currentTimeMillis() - startedAtEpochMillis).coerceAtLeast(0L)
                sessionState.value = current.copy(elapsedMillis = elapsed)
                delay(TIMER_TICK_MILLIS)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private companion object {
        const val TIMER_TICK_MILLIS = 1_000L
        const val INTENTION_MAX_LENGTH = 120
    }
}