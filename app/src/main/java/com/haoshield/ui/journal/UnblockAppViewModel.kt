package com.haoshield.ui.journal

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.util.AppLabelProvider
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.usecase.UnblockAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnblockAppUiState(
    val packageName: String = "",
    val appLabel: String = "",
    val intentionNote: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
    val hasActiveSession: Boolean = false,
)

@HiltViewModel
class UnblockAppViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val unblockAppUseCase: UnblockAppUseCase,
    appLabelProvider: AppLabelProvider,
) : ViewModel() {

    private val packageName: String = Uri.decode(
        checkNotNull(savedStateHandle.get<String>(PACKAGE_NAME_ARG)),
    )

    private val _uiState = MutableStateFlow(
        UnblockAppUiState(
            packageName = packageName,
            appLabel = appLabelProvider.getLabel(packageName),
        ),
    )
    val uiState: StateFlow<UnblockAppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSession = sessionManager.getActiveSession() != null
            _uiState.update { it.copy(hasActiveSession = hasSession) }
            if (!hasSession) {
                _uiState.update {
                    it.copy(errorMessage = "Start a protected session before unblocking an app.")
                }
            }
        }
    }

    fun onIntentionNoteChange(note: String) {
        if (note.length <= MAX_NOTE_LENGTH) {
            _uiState.update { it.copy(intentionNote = note, errorMessage = null) }
        }
    }

    fun onSubmitUnblock() {
        val current = _uiState.value
        if (current.isSubmitting || current.isCompleted) return

        if (current.intentionNote.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please write a short intention note.") }
            return
        }

        viewModelScope.launch {
            val session = sessionManager.getActiveSession()
            if (session == null) {
                _uiState.update {
                    it.copy(errorMessage = "No active session. Unblocks last only for the current session.")
                }
                return@launch
            }

            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            unblockAppUseCase(
                sessionId = session.id,
                packageName = packageName,
                journalNote = current.intentionNote.trim(),
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, isCompleted = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Unable to unblock app.",
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val PACKAGE_NAME_ARG = "packageName"
        const val MAX_NOTE_LENGTH = 240
    }
}