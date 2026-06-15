package com.haoshield.domain.usecase

import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.service.SessionManager
import javax.inject.Inject

class EndSessionUseCase @Inject constructor(
    private val sessionManager: SessionManager,
) {
    suspend operator fun invoke(
        method: SessionEndMethod,
        shieldTagId: String? = null,
    ): SessionEndResult = sessionManager.endSession(method, shieldTagId)
}