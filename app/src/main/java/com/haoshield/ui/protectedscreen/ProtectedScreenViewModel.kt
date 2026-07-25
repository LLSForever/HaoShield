package com.haoshield.ui.protectedscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.audio.AmbientMusicPlayer
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.repository.SettingsRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProtectedScreenViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val endSessionUseCase: EndSessionUseCase,
    private val ambientMusicPlayer: AmbientMusicPlayer,
    private val shieldTokenStore: ShieldTokenStore,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProtectedScreenUiState())
    val uiState: StateFlow<ProtectedScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProtectedScreenEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var quoteRotationJob: Job? = null
    private var countdownJob: Job? = null
    private var lastQuote: String? = null

    // Session defaults, read once when the screen opens.
    private var quotesEnabled: Boolean = true

    // Once the person sets or waves away the intention prompt, it doesn't return for this session.
    private var intentionPromptDismissed: Boolean = false

    // Latched true once the prompt first becomes eligible, so crossing the time window while the
    // person is mid-thought doesn't yank the field away — only dismissing or setting it hides it.
    private var intentionPromptActivated: Boolean = false

    init {
        applySessionDefaults()
        observeSession()
        observeQrToken()
    }

    private fun applySessionDefaults() {
        viewModelScope.launch {
            quotesEnabled = settingsRepository.observeQuotesEnabled().first()
            // Start ambient sound automatically if the user has it on by default.
            if (settingsRepository.observeAmbientSoundEnabled().first() && !ambientMusicPlayer.isPlaying()) {
                ambientMusicPlayer.play()
                _uiState.update { it.copy(isAmbientMusicPlaying = true) }
            }
        }
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

    fun onIntentionDraftChange(text: String) {
        if (text.length > INTENTION_MAX_LENGTH) return
        _uiState.update { it.copy(intentionDraft = text) }
    }

    fun onSubmitIntention() {
        intentionPromptDismissed = true
        val draft = _uiState.value.intentionDraft.trim()
        if (draft.isBlank()) {
            _uiState.update { it.copy(showIntentionPrompt = false) }
            return
        }
        viewModelScope.launch {
            sessionManager.setSessionIntention(draft)
            _uiState.update { it.copy(showIntentionPrompt = false) }
        }
    }

    fun onDismissIntentionPrompt() {
        intentionPromptDismissed = true
        _uiState.update { it.copy(showIntentionPrompt = false) }
    }

    fun onEndSessionClick() {
        if (_uiState.value.isEndingSession) return

        viewModelScope.launch {
            _uiState.update { it.copy(isEndingSession = true, endSessionHint = null) }

            when (val result = endSessionUseCase(SessionEndMethod.IN_APP)) {
                // Navigation is driven by observeSession() reacting to the session going null, so
                // both in-app and scan ends flow to the reflection screen through one path.
                is SessionEndResult.Ended -> Unit
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
                // observeSession() navigates Home (no reflection — the note is the reflection).
                is SessionEndResult.Ended -> {
                    _uiState.update {
                        it.copy(emergencyStep = null, emergencyNote = "", emergencyCountdownSeconds = 0)
                    }
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
                    // A normal end leaves a reflection summary; an emergency exit doesn't.
                    val event = if (sessionManager.getLastEndedSession() != null) {
                        ProtectedScreenEvent.NavigateToReflection
                    } else {
                        ProtectedScreenEvent.NavigateHome
                    }
                    _events.send(event)
                    return@collect
                }

                val intention = sessionState.session.intention
                // The prompt is *offered* only early, but once offered it stays until the person
                // sets or dismisses it — the window opens it, it doesn't slam it shut.
                if (intention == null &&
                    !intentionPromptDismissed &&
                    sessionState.elapsedMillis < INTENTION_PROMPT_WINDOW_MS
                ) {
                    intentionPromptActivated = true
                }
                _uiState.update {
                    it.copy(
                        formattedElapsedTime = ProtectedTimeFormatter.format(sessionState.elapsedMillis),
                        sessionMode = sessionState.session.mode,
                        isSessionActive = true,
                        isEndingSession = false,
                        intention = intention,
                        showIntentionPrompt = intentionPromptActivated &&
                            intention == null &&
                            !intentionPromptDismissed,
                    )
                }
                startQuoteRotation()
            }
        }
    }

    private fun startQuoteRotation() {
        if (!quotesEnabled) return
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
        const val INTENTION_MAX_LENGTH = 120
        const val INTENTION_PROMPT_WINDOW_MS = 120_000L
    }
}

sealed interface ProtectedScreenEvent {
    data object NavigateHome : ProtectedScreenEvent
    data object NavigateToReflection : ProtectedScreenEvent
}