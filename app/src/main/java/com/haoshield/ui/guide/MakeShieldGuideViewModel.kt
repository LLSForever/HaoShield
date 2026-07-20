package com.haoshield.ui.guide

import android.app.Activity
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.qr.QrCodeGenerator
import com.haoshield.data.qr.ShieldQr
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.domain.service.NfcManager
import com.haoshield.domain.service.ShieldScanHandler
import com.haoshield.domain.service.ShieldTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GuideStep {
    CONTENT,
    CHOOSE_METHOD,
    REGISTER,
    QR_DISPLAY,
    SUCCESS,
}

data class MakeShieldGuideUiState(
    val step: GuideStep = GuideStep.CONTENT,
    val isNfcAvailable: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val statusMessage: String? = null,
    val registeredUid: String? = null,
    val qrBitmap: Bitmap? = null,
    val isGeneratingQr: Boolean = false,
)

@HiltViewModel
class MakeShieldGuideViewModel @Inject constructor(
    private val nfcManager: NfcManager,
    private val shieldTokenStore: ShieldTokenStore,
    private val shieldScanHandler: ShieldScanHandler,
    private val qrCodeGenerator: QrCodeGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MakeShieldGuideUiState(
            isNfcAvailable = nfcManager.isNfcAvailable(),
            isNfcEnabled = nfcManager.isNfcEnabled(),
        ),
    )
    val uiState: StateFlow<MakeShieldGuideUiState> = _uiState.asStateFlow()

    val registeredUid: StateFlow<String?> =
        shieldTokenStore.observeRegisteredTokens()
            .map { tokens -> tokens.firstOrNull()?.id }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            registeredUid.collect { uid ->
                _uiState.update { it.copy(registeredUid = uid) }
            }
        }
        viewModelScope.launch {
            shieldScanHandler.observeScanResults().collect { result ->
                when (result) {
                    is ShieldScanResult.RegistrationComplete -> {
                        _uiState.update {
                            it.copy(
                                step = GuideStep.SUCCESS,
                                statusMessage = MakeShieldGuideContent.registerSuccess,
                                registeredUid = result.token.id,
                            )
                        }
                    }
                    is ShieldScanResult.Failed -> {
                        if (_uiState.value.step == GuideStep.REGISTER) {
                            _uiState.update { it.copy(statusMessage = result.reason) }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onBeginRegistration() {
        _uiState.update {
            it.copy(step = GuideStep.CHOOSE_METHOD, statusMessage = null)
        }
    }

    fun onChooseNfcMethod() {
        _uiState.update {
            it.copy(
                step = GuideStep.REGISTER,
                statusMessage = null,
                isNfcAvailable = nfcManager.isNfcAvailable(),
                isNfcEnabled = nfcManager.isNfcEnabled(),
            )
        }
    }

    fun onChooseQrMethod() {
        _uiState.update { it.copy(isGeneratingQr = true, statusMessage = null) }
        viewModelScope.launch {
            val payload = ShieldQr.newPayload()
            shieldTokenStore.setPendingQrPayload(payload)
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeGenerator.generate(payload)
            }
            _uiState.update {
                it.copy(
                    step = GuideStep.QR_DISPLAY,
                    qrBitmap = bitmap,
                    isGeneratingQr = false,
                )
            }
        }
    }

    fun onEnterRegistrationMode(activity: Activity) {
        if (nfcManager.isNfcAvailable() && nfcManager.isNfcEnabled()) {
            nfcManager.enableForegroundReader(activity, ScanMode.REGISTRATION)
        }
    }

    fun onLeaveRegistrationMode(activity: Activity) {
        if (nfcManager.isNfcAvailable() && nfcManager.isNfcEnabled()) {
            nfcManager.enableForegroundReader(activity, ScanMode.SESSION)
        }
    }

    fun onBackToContent() {
        _uiState.update {
            it.copy(step = GuideStep.CONTENT, statusMessage = null, qrBitmap = null)
        }
    }

    fun onBackToChooseMethod() {
        _uiState.update {
            it.copy(step = GuideStep.CHOOSE_METHOD, statusMessage = null, qrBitmap = null)
        }
    }

    fun onDismissSuccess() {
        _uiState.update {
            it.copy(step = GuideStep.CONTENT, statusMessage = null, qrBitmap = null)
        }
    }
}