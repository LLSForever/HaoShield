package com.haoshield.data.service

import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.Session
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.repository.JournalRepository
import com.haoshield.domain.service.Clock
import com.haoshield.domain.service.SessionSnapshot
import com.haoshield.domain.service.SessionStore
import com.haoshield.domain.service.ShieldTokenStore
import com.haoshield.domain.service.StrictBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Time the tests control, so rules about expiry and staleness can actually be proved. */
class TestClock(var now: Long = 1_700_000_000_000) : Clock {
    override fun nowMillis(): Long = now

    fun advanceBy(millis: Long) {
        now += millis
    }
}

/** The session written down, in memory. */
class FakeSessionStore(initial: SessionSnapshot? = null) : SessionStore {
    private val snapshot = MutableStateFlow(initial)

    var clearCount: Int = 0
        private set

    val current: SessionSnapshot? get() = snapshot.value

    override fun observePersistedSession(): Flow<SessionSnapshot?> = snapshot.asStateFlow()

    override suspend fun persistActiveSession(
        session: Session,
        temporarilyAllowedPackages: Map<String, Long>,
    ) {
        snapshot.value = SessionSnapshot(session, temporarilyAllowedPackages)
    }

    override suspend fun persistIntention(intention: String) {
        snapshot.value = snapshot.value?.let {
            it.copy(session = it.session.copy(intention = intention))
        }
    }

    override suspend fun persistAllowedPackages(packages: Map<String, Long>) {
        snapshot.value = snapshot.value?.copy(temporarilyAllowedPackages = packages)
    }

    override suspend fun clearSession() {
        clearCount++
        snapshot.value = null
    }
}

/** Records what the session rules asked of strict blocking, and can refuse like a denied root. */
class FakeStrictBlocking : StrictBlocking {
    val releasedPackages = mutableListOf<Pair<String, Long?>>()
    var applyCount: Int = 0
        private set
    var releaseAllCount: Int = 0
        private set

    /** Simulates root being denied part-way through a session. */
    var failOnRelease: Boolean = false

    override suspend fun applyForSession() {
        applyCount++
    }

    override suspend fun releaseAll() {
        releaseAllCount++
    }

    override suspend fun release(packageName: String, resuspendAfterMillis: Long?) {
        if (failOnRelease) error("root denied")
        releasedPackages += packageName to resuspendAfterMillis
    }
}

class FakeJournalRepository : JournalRepository {
    val saved = mutableListOf<JournalEntry>()

    override fun observeEntries(): Flow<List<JournalEntry>> = MutableStateFlow(saved).asStateFlow()

    override suspend fun getEntry(id: Long): JournalEntry? = saved.firstOrNull { it.id == id }

    override suspend fun saveEntry(entry: JournalEntry): JournalEntry {
        saved += entry
        return entry
    }

    override suspend fun pruneEntriesBefore(cutoffEpochMillis: Long) {
        saved.removeAll { it.createdAtEpochMillis < cutoffEpochMillis }
    }
}

class FakeShieldTokenStore(private var registered: ShieldToken? = null) : ShieldTokenStore {
    private var pendingQr: String? = null

    override fun observeRegisteredTokens(): Flow<List<ShieldToken>> =
        MutableStateFlow(listOfNotNull(registered)).asStateFlow()

    override suspend fun getRegisteredTokens(): List<ShieldToken> = listOfNotNull(registered)

    override suspend fun hasAnyRegisteredToken(): Boolean = registered != null

    override suspend fun registerToken(token: ShieldToken): Result<Unit> = runCatching {
        registered = token.normalized()
    }

    override suspend fun clearToken(kind: ShieldTokenKind): Result<Unit> = runCatching {
        if (registered?.kind == kind) registered = null
    }

    override suspend fun validate(token: ShieldToken): Boolean =
        registered != null && registered == token.normalized()

    override suspend fun setPendingQrPayload(payload: String) {
        pendingQr = payload
    }

    override suspend fun getPendingQrPayload(): String? = pendingQr

    override suspend fun clearPendingQrPayload() {
        pendingQr = null
    }
}
