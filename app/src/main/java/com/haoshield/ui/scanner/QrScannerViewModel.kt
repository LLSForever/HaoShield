package com.haoshield.ui.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.qr.ShieldQr
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.service.ShieldScanHandler
import com.haoshield.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrScannerUiState(
    val hint: String? = null,
)

sealed interface QrScannerEvent {
    /** A shield code was scanned and handled; the caller should close the scanner. */
    data class Finished(val result: ShieldScanResult) : QrScannerEvent
}

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val shieldScanHandler: ShieldScanHandler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val scanMode: ScanMode =
        savedStateHandle.get<String>(Route.QrScanner.ARG_SCAN_MODE)
            ?.let { runCatching { ScanMode.valueOf(it) }.getOrNull() }
            ?: ScanMode.SESSION

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    private val _events = Channel<QrScannerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    @Volatile
    private var handled = false

    /** Called by the analyzer for each decoded QR value. Acts once on the first shield code. */
    fun onQrDetected(rawValue: String) {
        if (handled) return
        if (!ShieldQr.isShieldPayload(rawValue)) {
            _uiState.update { it.copy(hint = "That's not a Hǎo Shield code.") }
            return
        }
        handled = true
        viewModelScope.launch {
            val result = shieldScanHandler.handleScan(
                token = ShieldToken(ShieldTokenKind.QR, rawValue),
                mode = scanMode,
            )
            when (result) {
                // A transient miss (debounced, or "not the code you just made") shouldn't close the
                // scanner — show the reason inline and let the camera keep trying.
                is ShieldScanResult.Failed,
                is ShieldScanResult.InvalidShield,
                ShieldScanResult.NoShieldRegistered,
                ShieldScanResult.SoftwareSessionActive -> {
                    _uiState.update { it.copy(hint = result.hintMessage()) }
                    delay(RETRY_COOLDOWN_MILLIS)
                    handled = false
                }
                is ShieldScanResult.SessionStarted,
                is ShieldScanResult.SessionEnded,
                is ShieldScanResult.RegistrationComplete ->
                    _events.send(QrScannerEvent.Finished(result))
            }
        }
    }

    private fun ShieldScanResult.hintMessage(): String = when (this) {
        is ShieldScanResult.Failed -> reason
        is ShieldScanResult.InvalidShield -> "That isn't your registered Hǎo Shield."
        ShieldScanResult.NoShieldRegistered -> "No Hǎo Shield is registered yet."
        ShieldScanResult.SoftwareSessionActive -> "A Software session is already running."
        else -> "Try again."
    }

    private companion object {
        const val RETRY_COOLDOWN_MILLIS = 1_200L
    }
}
