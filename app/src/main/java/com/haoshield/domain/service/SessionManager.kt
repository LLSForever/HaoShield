package com.haoshield.domain.service

import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.SessionState
import kotlinx.coroutines.flow.Flow

interface SessionManager {
    fun observeSessionState(): Flow<SessionState?>

    fun observeElapsedMillis(): Flow<Long>

    suspend fun getSessionState(): SessionState?

    suspend fun getActiveSession(): Session?

    suspend fun startSession(mode: SessionMode): Session

    suspend fun endSession(
        method: SessionEndMethod,
        shieldTagId: String? = null,
    ): SessionEndResult

    suspend fun allowAppTemporarily(
        packageName: String,
        journalNote: String,
    ): Result<Unit>

    suspend fun isAppTemporarilyAllowed(packageName: String): Boolean

    suspend fun restorePersistedSession()
}