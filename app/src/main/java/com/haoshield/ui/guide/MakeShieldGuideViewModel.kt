package com.haoshield.ui.guide

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.domain.model.NfcReaderMode
import com.haoshield.domain.model.NfcTapResult
import com.haoshield.domain.service.NfcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GuideStep {
    CONTENT,
    REGISTER,
    SUCCESS,
}

data class MakeShieldGuideUiState(
    val step: GuideStep = GuideStep.CONTENT,
    val isNfcAvailable: Boolean = false,
    val isNfcEnabled: Boolean = false,
    val statusMessage: String? = null,
    val registeredUid: String? = null,
)

@HiltViewModel
class MakeShieldGuideViewModel @Inject constructor(
    private val nfcManager: NfcManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MakeShieldGuideUiState(
            isNfcAvailable = nfcManager.isNfcAvailable(),
            isNfcEnabled = nfcManager.isNfcEnabled(),
        ),
    )
    val uiState: StateFlow<MakeShieldGuideUiState> = _uiState.asStateFlow()

    val registeredUid: StateFlow<String?> =
        nfcManager.observeRegisteredShieldUid()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            registeredUid.collect { uid ->
                _uiState.update { it.copy(registeredUid = uid) }
            }
        }
        viewModelScope.launch {
            nfcManager.observeTapResults().collect { result ->
                when (result) {
                    is NfcTapResult.RegistrationComplete -> {
                        _uiState.update {
                            it.copy(
                                step = GuideStep.SUCCESS,
                                statusMessage = MakeShieldGuideContent.registerSuccess,
                                registeredUid = result.uid,
                            )
                        }
                    }
                    is NfcTapResult.Failed -> {
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
            it.copy(
                step = GuideStep.REGISTER,
                statusMessage = null,
                isNfcAvailable = nfcManager.isNfcAvailable(),
                isNfcEnabled = nfcManager.isNfcEnabled(),
            )
        }
    }

    fun onEnterRegistrationMode(activity: Activity) {
        if (nfcManager.isNfcAvailable() && nfcManager.isNfcEnabled()) {
            nfcManager.enableForegroundReader(activity, NfcReaderMode.REGISTRATION)
        }
    }

    fun onLeaveRegistrationMode(activity: Activity) {
        if (nfcManager.isNfcAvailable() && nfcManager.isNfcEnabled()) {
            nfcManager.enableForegroundReader(activity, NfcReaderMode.SESSION)
        }
    }

    fun onBackToContent() {
        _uiState.update {
            it.copy(step = GuideStep.CONTENT, statusMessage = null)
        }
    }

    fun onDismissSuccess() {
        _uiState.update {
            it.copy(step = GuideStep.CONTENT, statusMessage = null)
        }
    }
}