package com.haoshield.ui.protectedscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.audio.AmbientMusicPlayer
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.service.ShieldTokenStore
import com.haoshield.domain.usecase.EndSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProtectedScreenViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val endSessionUseCase: EndSessionUseCase,
    private val ambientMusicPlayer: AmbientMusicPlayer,
    private val shieldTokenStore: ShieldTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProtectedScreenUiState())
    val uiState: StateFlow<ProtectedScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProtectedScreenEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var quoteRotationJob: Job? = null
    private var countdownJob: Job? = null
    private var lastQuote: String? = null

    init {
        observeSession()
        observeQrToken()
    }

    fun onToggleAmbientMusic() {
        if (ambientMusicPlayer.isPlaying()) {
            ambientMusicPlayer.pause()
            _uiState.update { it.copy(isAmbientMusicPlaying = false) }
        } else {
            ambientMusicPlayer.play()
            _uiState.update { it.copy(isAmbientMusicPlaying = true) }
        }
    }

    fun onEndSessionClick() {
        if (_uiState.value.isEndingSession) return

        viewModelScope.launch {
            _uiState.update { it.copy(isEndingSession = true, endSessionHint = null) }

            when (val result = endSessionUseCase(SessionEndMethod.IN_APP)) {
                is SessionEndResult.Ended -> {
                    stopAmbientMusic()
                    _events.send(ProtectedScreenEvent.NavigateHome)
                }
                is SessionEndResult.RequiresShieldScan -> {
                    _uiState.update {
                        it.copy(
                            isEndingSession = false,
                            endSessionHint = result.message,
                        )
                    }
                }
                is SessionEndResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            isEndingSession = false,
                            endSessionHint = result.reason,
                        )
                    }
                }
            }
        }
    }

    fun onRequestEmergencyExit() {
        _uiState.update {
            it.copy(
                emergencyStep = EmergencyExitStep.WRITING_NOTE,
                emergencyNote = "",
                endSessionHint = null,
            )
        }
    }

    fun onEmergencyNoteChange(note: String) {
        if (note.length > EMERGENCY_NOTE_MAX_LENGTH) return
        _uiState.update { it.copy(emergencyNote = note) }
    }

    fun onCancelEmergencyExit() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.update {
            it.copy(
                emergencyStep = null,
                emergencyNote = "",
                emergencyCountdownSeconds = 0,
            )
        }
    }

    fun onStartEmergencyCountdown() {
        val note = _uiState.value.emergencyNote
        if (note.isBlank() || countdownJob?.isActive == true) return

        _uiState.update {
            it.copy(
                emergencyStep = EmergencyExitStep.COUNTDOWN,
                emergencyCountdownSeconds = EMERGENCY_COUNTDOWN_SECONDS,
            )
        }

        countdownJob = viewModelScope.launch {
            var remaining = EMERGENCY_COUNTDOWN_SECONDS
            while (remaining > 0) {
                delay(1_000L)
                remaining -= 1
                _uiState.update { it.copy(emergencyCountdownSeconds = remaining) }
            }

            when (val result = sessionManager.endShieldSessionByEmergency(note)) {
                is SessionEndResult.Ended -> {
                    stopAmbientMusic()
                    _uiState.update {
                        it.copy(emergencyStep = null, emergencyNote = "", emergencyCountdownSeconds = 0)
                    }
                    _events.send(ProtectedScreenEvent.NavigateHome)
                }
                is SessionEndResult.RequiresShieldScan -> {
                    _uiState.update {
                        it.copy(
                            emergencyStep = null,
                            emergencyCountdownSeconds = 0,
                            endSessionHint = result.message,
                        )
                    }
                }
                is SessionEndResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            emergencyStep = null,
                            emergencyCountdownSeconds = 0,
                            endSessionHint = result.reason,
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        ambientMusicPlayer.pause()
        super.onCleared()
    }

    private fun observeQrToken() {
        viewModelScope.launch {
            shieldTokenStore.observeRegisteredTokens().collect { tokens ->
                val hasQr = tokens.any { it.kind == ShieldTokenKind.QR }
                _uiState.update { it.copy(hasQrToken = hasQr) }
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.observeSessionState().collect { sessionState ->
                if (sessionState == null) {
                    stopQuoteRotation()
                    stopAmbientMusic()
                    _events.send(ProtectedScreenEvent.NavigateHome)
                    return@collect
                }

                _uiState.update {
                    it.copy(
                        formattedElapsedTime = ProtectedTimeFormatter.format(sessionState.elapsedMillis),
                        sessionMode = sessionState.session.mode,
                        isSessionActive = true,
                        isEndingSession = false,
                    )
                }
                startQuoteRotation()
            }
        }
    }

    private fun startQuoteRotation() {
        if (quoteRotationJob?.isActive == true) return

        quoteRotationJob = viewModelScope.launch {
            delay(INITIAL_QUOTE_DELAY_MS)
            while (true) {
                if (!_uiState.value.isSessionActive) break

                val quote = pickNextQuote()
                _uiState.update { it.copy(currentQuote = quote, quoteVisible = true) }
                delay(randomQuoteVisibleDuration())
                _uiState.update { it.copy(quoteVisible = false) }
                delay(QUOTE_INTERVAL_MS)
            }
        }
    }

    private fun stopQuoteRotation() {
        quoteRotationJob?.cancel()
        quoteRotationJob = null
        _uiState.update { it.copy(quoteVisible = false, currentQuote = null) }
    }

    private fun stopAmbientMusic() {
        ambientMusicPlayer.pause()
        _uiState.update { it.copy(isAmbientMusicPlaying = false) }
    }

    private fun pickNextQuote(): String {
        val candidates = ProtectedQuotes.filterNot { it == lastQuote }
        val quote = candidates.random()
        lastQuote = quote
        return quote
    }

    private fun randomQuoteVisibleDuration(): Long =
        Random.nextLong(QUOTE_VISIBLE_MIN_MS, QUOTE_VISIBLE_MAX_MS + 1)

    private companion object {
        const val INITIAL_QUOTE_DELAY_MS = 30_000L
        const val QUOTE_VISIBLE_MIN_MS = 20_000L
        const val QUOTE_VISIBLE_MAX_MS = 30_000L
        const val QUOTE_INTERVAL_MS = 50_000L
        const val EMERGENCY_COUNTDOWN_SECONDS = 60
        const val EMERGENCY_NOTE_MAX_LENGTH = 240
    }
}

sealed interface ProtectedScreenEvent {
    data object NavigateHome : ProtectedScreenEvent
}