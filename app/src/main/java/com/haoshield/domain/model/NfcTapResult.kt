package com.haoshield.domain.model

sealed interface NfcTapResult {
    data class RegistrationComplete(val uid: String) : NfcTapResult

    data class SessionStarted(val session: Session) : NfcTapResult

    data class SessionEnded(val session: Session) : NfcTapResult

    data class InvalidShield(val uid: String) : NfcTapResult

    data object NoShieldRegistered : NfcTapResult

    data object SoftwareSessionActive : NfcTapResult

    data class Failed(val reason: String) : NfcTapResult
}