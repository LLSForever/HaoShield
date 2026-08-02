package com.haoshield.domain.service

import com.haoshield.domain.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * A session as it survives the process dying: the session itself, and which apps were let through
 * and until when.
 */
data class SessionSnapshot(
    val session: Session,
    val temporarilyAllowedPackages: Map<String, Long>,
)

/**
 * Where a live session is written down so it outlives the process.
 *
 * An interface rather than the DataStore class directly, for two reasons: the session rules can
 * then be tested against an in-memory store with no Android at all, and a second platform can
 * supply its own writing-down without the rules knowing.
 */
interface SessionStore {
    fun observePersistedSession(): Flow<SessionSnapshot?>

    suspend fun persistActiveSession(
        session: Session,
        temporarilyAllowedPackages: Map<String, Long>,
    )

    suspend fun persistIntention(intention: String)

    suspend fun persistAllowedPackages(packages: Map<String, Long>)

    suspend fun clearSession()
}
