package com.haoshield.domain.model

sealed interface SessionEndResult {
    data class Ended(val session: Session) : SessionEndResult

    data class RequiresShieldScan(
        val message: String = "Scan your Hǎo Shield to end this session.",
    ) : SessionEndResult

    data class Failed(val reason: String) : SessionEndResult
}