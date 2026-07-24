package com.haoshield.ui.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalEntryType
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.usecase.SaveJournalEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReflectionUiState(
    val minutesProtected: Long = 0L,
    val intention: String? = null,
    val reflectionText: String = "",
)

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
) : ViewModel() {

    private val summary = sessionManager.getLastEndedSession()
    private val sessionId: Long = summary?.sessionId ?: 0L

    private val _uiState = MutableStateFlow(
        ReflectionUiState(
            minutesProtected = (summary?.durationMillis ?: 0L) / 60_000L,
            intention = summary?.intention?.takeIf { it.isNotBlank() },
        ),
    )
    val uiState: StateFlow<ReflectionUiState> = _uiState.asStateFlow()

    private val _events = Channel<Unit>(Channel.BUFFERED)
    /** Emits when the screen should return to Home. */
    val doneEvents = _events.receiveAsFlow()

    init {
        // If there's no summary (emergency exit, process death, or a stray navigation), don't
        // present an empty reflection — go straight Home.
        if (summary == null) {
            viewModelScope.launch { _events.send(Unit) }
        }
    }

    fun onReflectionChange(text: String) {
        if (text.length > REFLECTION_MAX_LENGTH) return
        _uiState.update { it.copy(reflectionText = text) }
    }

    fun onDone() {
        viewModelScope.launch {
            val reflection = _uiState.value.reflectionText.trim()
            val intention = _uiState.value.intention?.trim().orEmpty()
            val content = when {
                reflection.isNotEmpty() && intention.isNotEmpty() -> "For: $intention\n$reflection"
                reflection.isNotEmpty() -> reflection
                intention.isNotEmpty() -> intention
                else -> null
            }
            if (content != null) {
                saveJournalEntryUseCase(
                    JournalEntry(
                        content = content,
                        createdAtEpochMillis = System.currentTimeMillis(),
                        sessionId = sessionId,
                        type = JournalEntryType.REFLECTION,
                    ),
                )
            }
            sessionManager.clearLastEndedSession()
            _events.send(Unit)
        }
    }

    private companion object {
        const val REFLECTION_MAX_LENGTH = 240
    }
}
