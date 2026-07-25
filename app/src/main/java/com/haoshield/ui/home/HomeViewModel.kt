package com.haoshield.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.permission.ShieldPermissions
import com.haoshield.domain.model.BlockingMode
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.repository.SettingsRepository
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.service.ShieldTokenStore
import com.haoshield.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomePhase { Idle, AwaitingShield, Active }

data class HomeUiState(
    val phase: HomePhase = HomePhase.Idle,
    val mode: BlockingMode = BlockingMode.SOFTWARE,
    val hasNfcToken: Boolean = false,
    val hasQrToken: Boolean = false,
    val elapsedMinutes: Long = 0,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasAnyToken: Boolean get() = hasNfcToken || hasQrToken
}

sealed interface HomeEvent {
    data object NavigateToProtected : HomeEvent
    data object NavigateToSetup : HomeEvent
    data object NavigateToGuide : HomeEvent
    data class NavigateToScanner(val mode: ScanMode) : HomeEvent
}

private data class LocalHomeState(
    val awaiting: Boolean = false,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val shieldPermissions: ShieldPermissions,
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    shieldTokenStore: ShieldTokenStore,
) : ViewModel() {

    private val local = MutableStateFlow(LocalHomeState())

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.observeBlockingMode(),
        sessionManager.observeSessionState(),
        shieldTokenStore.observeRegisteredTokens(),
        local,
    ) { mode, session, tokens, localState ->
        // A live session wins over any awaiting-shield prompt.
        if (session != null && localState.awaiting) {
            local.update { it.copy(awaiting = false) }
        }
        HomeUiState(
            phase = when {
                session != null -> HomePhase.Active
                localState.awaiting -> HomePhase.AwaitingShield
                else -> HomePhase.Idle
            },
            mode = mode,
            hasNfcToken = tokens.any { it.kind == ShieldTokenKind.NFC },
            hasQrToken = tokens.any { it.kind == ShieldTokenKind.QR },
            elapsedMinutes = (session?.elapsedMillis ?: 0L) / 60_000L,
            isStarting = localState.isStarting,
            errorMessage = localState.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onGlyphClick() {
        val state = uiState.value
        when {
            state.phase == HomePhase.Active -> emit(HomeEvent.NavigateToProtected)
            state.mode == BlockingMode.SOFTWARE -> startSoftwareSession()
            else -> beginShieldFlow(state)
        }
    }

    private fun beginShieldFlow(state: HomeUiState) {
        when {
            // No Shield registered yet — send the user to make and register one, never dead-end.
            !state.hasAnyToken -> emit(HomeEvent.NavigateToGuide)
            !shieldPermissions.isReady() -> emit(HomeEvent.NavigateToSetup)
            // NFC reader is already armed by MainActivity; just show the prompt and wait for a tap.
            else -> local.update { it.copy(awaiting = true, errorMessage = null) }
        }
    }

    private fun startSoftwareSession() {
        if (local.value.isStarting) return
        if (!shieldPermissions.isReady()) {
            emit(HomeEvent.NavigateToSetup)
            return
        }
        viewModelScope.launch {
            local.update { it.copy(isStarting = true, errorMessage = null) }
            runCatching { startSessionUseCase(SessionMode.SOFTWARE) }.fold(
                onSuccess = {
                    local.update { it.copy(isStarting = false) }
                    _events.send(HomeEvent.NavigateToProtected)
                },
                onFailure = { error ->
                    local.update {
                        it.copy(
                            isStarting = false,
                            errorMessage = error.message ?: "Unable to start session.",
                        )
                    }
                },
            )
        }
    }

    /** Discovery affordance for users who haven't made a Shield yet. */
    fun onMakeShield() {
        emit(HomeEvent.NavigateToGuide)
    }

    /** User wants to end the "hold your Shield" prompt without tapping. */
    fun onCancelAwaiting() {
        local.update { it.copy(awaiting = false) }
    }

    /** In AwaitingShield, the user chooses to scan a printed Shield instead of tapping an NFC tag. */
    fun onScanPrintedShield() {
        emit(HomeEvent.NavigateToScanner(ScanMode.SESSION))
    }

    private fun emit(event: HomeEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
