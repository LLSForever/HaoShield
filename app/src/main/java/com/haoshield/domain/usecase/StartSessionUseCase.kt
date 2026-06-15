package com.haoshield.domain.usecase

import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.service.SessionManager
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val sessionManager: SessionManager,
) {
    suspend operator fun invoke(mode: SessionMode): Session =
        sessionManager.startSession(mode)
}