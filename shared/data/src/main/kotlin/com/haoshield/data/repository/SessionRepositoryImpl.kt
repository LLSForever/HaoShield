package com.haoshield.data.repository

import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.repository.SessionRepository
import com.haoshield.domain.service.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
) : SessionRepository {

    override fun observeActiveSession(): Flow<Session?> =
        sessionManager.observeSessionState().map { it?.session }

    override suspend fun getActiveSession(): Session? =
        sessionManager.getActiveSession()

    override suspend fun startSession(mode: SessionMode): Session =
        sessionManager.startSession(mode)

    override suspend fun endSession(sessionId: Long): Session? {
        val result = sessionManager.endSession(SessionEndMethod.IN_APP)
        return when (result) {
            is SessionEndResult.Ended -> {
                if (result.session.id == sessionId) result.session else null
            }
            else -> null
        }
    }
}