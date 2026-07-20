package com.haoshield.domain.model

/** The outcome of a shield scan, whether from an NFC tap or a QR camera scan. */
sealed interface ShieldScanResult {
    data class RegistrationComplete(val token: ShieldToken) : ShieldScanResult

    data class SessionStarted(val session: Session) : ShieldScanResult

    data class SessionEnded(val session: Session) : ShieldScanResult

    data class InvalidShield(val token: ShieldToken) : ShieldScanResult

    data object NoShieldRegistered : ShieldScanResult

    data object SoftwareSessionActive : ShieldScanResult

    data class Failed(val reason: String) : ShieldScanResult
}
