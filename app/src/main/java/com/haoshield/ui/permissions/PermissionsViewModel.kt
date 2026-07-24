package com.haoshield.ui.permissions

import androidx.lifecycle.ViewModel
import com.haoshield.data.permission.ShieldPermissions
import com.haoshield.domain.service.NfcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PermissionsUiState(
    val isAccessibilityEnabled: Boolean = false,
    val canDrawOverlay: Boolean = false,
    val isNfcAvailable: Boolean = false,
    val isNfcEnabled: Boolean = false,
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val shieldPermissions: ShieldPermissions,
    private val nfcManager: NfcManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    /** Re-read live system state — called whenever the screen resumes. */
    fun refresh() {
        _uiState.update {
            it.copy(
                isAccessibilityEnabled = shieldPermissions.isAccessibilityServiceEnabled(),
                canDrawOverlay = shieldPermissions.canDrawOverlay(),
                isNfcAvailable = nfcManager.isNfcAvailable(),
                isNfcEnabled = nfcManager.isNfcEnabled(),
            )
        }
    }
}
