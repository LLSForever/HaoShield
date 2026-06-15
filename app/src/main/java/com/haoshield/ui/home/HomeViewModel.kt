package com.haoshield.ui.home

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

data class HomeUiState(
    val isStartingSoftwareSession: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val shieldPermissions: ShieldPermissions,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onSoftwareModeClick() {
        if (_uiState.value.isStartingSoftwareSession) return

        // A session with no blocking permissions would just run a timer. Send the user to set up
        // the Shield first; once it's ready, the same tap starts the session directly.
        if (!shieldPermissions.isReady()) {
            viewModelScope.launch { _events.send(HomeEvent.NavigateToSetup) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isStartingSoftwareSession = true, errorMessage = null) }
            runCatching {
                startSessionUseCase(SessionMode.SOFTWARE)
            }.fold(
                onSuccess = {
                    _uiState.update { it.copy(isStartingSoftwareSession = false) }
                    _events.send(HomeEvent.NavigateToProtected)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isStartingSoftwareSession = false,
                            errorMessage = error.message ?: "Unable to start session.",
                        )
                    }
                },
            )
        }
    }
}

sealed interface HomeEvent {
    data object NavigateToProtected : HomeEvent
    data object NavigateToSetup : HomeEvent
}