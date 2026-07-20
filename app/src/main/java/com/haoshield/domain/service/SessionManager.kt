package com.haoshield.domain.service

import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.SessionState
import com.haoshield.domain.model.ShieldToken
import kotlinx.coroutines.flow.Flow

interface SessionManager {
    fun observeSessionState(): Flow<SessionState?>

    fun observeElapsedMillis(): Flow<Long>

    suspend fun getSessionState(): SessionState?

    suspend fun getActiveSession(): Session?

    suspend fun startSession(mode: SessionMode): Session

    suspend fun endSession(
        method: SessionEndMethod,
        shieldToken: ShieldToken? = null,
    ): SessionEndResult

    /**
     * Ends an active Shield Mode session without the physical token, after the friction of a
     * written intention. Records the note to the journal as one atomic operation.
     */
    suspend fun endShieldSessionByEmergency(note: String): SessionEndResult

    suspend fun allowAppTemporarily(
        packageName: String,
        journalNote: String,
    ): Result<Unit>

    suspend fun isAppTemporarilyAllowed(packageName: String): Boolean

    suspend fun restorePersistedSession()
}