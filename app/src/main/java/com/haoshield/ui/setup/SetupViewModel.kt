package com.haoshield.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.permission.ShieldPermissions
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val isAccessibilityEnabled: Boolean = false,
    val canDrawOverlay: Boolean = false,
    val isStartingSession: Boolean = false,
) {
    val isReady: Boolean get() = isAccessibilityEnabled && canDrawOverlay
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val shieldPermissions: ShieldPermissions,
    private val startSessionUseCase: StartSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _events = Channel<SetupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Re-read the live permission state — called whenever the screen resumes. */
    fun refresh() {
        _uiState.update {
            it.copy(
                isAccessibilityEnabled = shieldPermissions.isAccessibilityServiceEnabled(),
                canDrawOverlay = shieldPermissions.canDrawOverlay(),
            )
        }
    }

    fun onStartSoftwareSession() {
        val state = _uiState.value
        if (state.isStartingSession || !state.isReady) return

        viewModelScope.launch {
            _uiState.update { it.copy(isStartingSession = true) }
            runCatching { startSessionUseCase(SessionMode.SOFTWARE) }
                .onSuccess {
                    _uiState.update { it.copy(isStartingSession = false) }
                    _events.send(SetupEvent.NavigateToProtected)
                }
                .onFailure {
                    _uiState.update { it.copy(isStartingSession = false) }
                }
        }
    }
}

sealed interface SetupEvent {
    data object NavigateToProtected : SetupEvent
}
