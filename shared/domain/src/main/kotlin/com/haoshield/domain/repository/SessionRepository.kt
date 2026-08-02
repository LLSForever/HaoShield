package com.haoshield.domain.repository

import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionMode
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeActiveSession(): Flow<Session?>

    suspend fun getActiveSession(): Session?

    suspend fun startSession(mode: SessionMode): Session

    suspend fun endSession(sessionId: Long): Session?
}