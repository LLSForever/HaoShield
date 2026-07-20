package com.haoshield.ui.shieldmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.permission.ShieldPermissions
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.service.NfcManager
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.service.ShieldTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ShieldModeUiState(
    val hasNfcToken: Boolean = false,
    val hasQrToken: Boolean = false,
    val isNfcHardwareAvailable: Boolean = false,
    val activeSessionMode: SessionMode? = null,
    val permissionsReady: Boolean = false,
) {
    val hasAnyToken: Boolean get() = hasNfcToken || hasQrToken
    val hasActiveSession: Boolean get() = activeSessionMode != null
}

@HiltViewModel
class ShieldModeViewModel @Inject constructor(
    shieldTokenStore: ShieldTokenStore,
    sessionManager: SessionManager,
    nfcManager: NfcManager,
    private val shieldPermissions: ShieldPermissions,
) : ViewModel() {

    private val nfcHardwareAvailable = nfcManager.isNfcAvailable()
    private val permissionsReady = MutableStateFlow(shieldPermissions.isReady())

    val uiState: StateFlow<ShieldModeUiState> = combine(
        shieldTokenStore.observeRegisteredTokens(),
        sessionManager.observeSessionState(),
        permissionsReady,
    ) { tokens, sessionState, ready ->
        ShieldModeUiState(
            hasNfcToken = tokens.any { it.kind == ShieldTokenKind.NFC },
            hasQrToken = tokens.any { it.kind == ShieldTokenKind.QR },
            isNfcHardwareAvailable = nfcHardwareAvailable,
            activeSessionMode = sessionState?.session?.mode,
            permissionsReady = ready,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ShieldModeUiState(isNfcHardwareAvailable = nfcHardwareAvailable),
    )

    /** Re-check permissions when returning from system settings. */
    fun refresh() {
        permissionsReady.value = shieldPermissions.isReady()
    }
}
