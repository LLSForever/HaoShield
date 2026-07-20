package com.haoshield.ui.protectedscreen

import com.haoshield.domain.model.SessionMode

enum class EmergencyExitStep {
    WRITING_NOTE,
    COUNTDOWN,
}

data class ProtectedScreenUiState(
    val formattedElapsedTime: String = "00:00",
    val sessionMode: SessionMode? = null,
    val isSessionActive: Boolean = false,
    val isAmbientMusicPlaying: Boolean = false,
    val currentQuote: String? = null,
    val quoteVisible: Boolean = false,
    val endSessionHint: String? = null,
    val isEndingSession: Boolean = false,
    val emergencyStep: EmergencyExitStep? = null,
    val emergencyNote: String = "",
    val emergencyCountdownSeconds: Int = 0,
    val hasQrToken: Boolean = false,
) {
    val protectionMessage: String
        get() = when (sessionMode) {
            SessionMode.SOFTWARE -> "You are protecting your attention."
            SessionMode.SHIELD -> "Your Shield is with you."
            null -> "Returning to yourself."
        }

    val endButtonLabel: String
        get() = when (sessionMode) {
            SessionMode.SHIELD -> "End with Shield"
            SessionMode.SOFTWARE, null -> "End session"
        }
}