package com.haoshield.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.domain.model.BlockingMode
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.repository.BlockingRepository
import com.haoshield.domain.repository.SettingsRepository
import com.haoshield.domain.service.ShieldTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlockedGroupSummary(
    val name: String,
    val count: Int,
)

data class SettingsUiState(
    val mode: BlockingMode = BlockingMode.SOFTWARE,
    val hasNfcToken: Boolean = false,
    val hasQrToken: Boolean = false,
    val blockedGroups: List<BlockedGroupSummary> = emptyList(),
    val ambientSound: Boolean = true,
    val quotes: Boolean = true,
) {
    val hasAnyToken: Boolean get() = hasNfcToken || hasQrToken
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    shieldTokenStore: ShieldTokenStore,
    blockingRepository: BlockingRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeBlockingMode(),
        shieldTokenStore.observeRegisteredTokens(),
        blockingRepository.observeBlockedGroups(),
        settingsRepository.observeAmbientSoundEnabled(),
        settingsRepository.observeQuotesEnabled(),
    ) { mode, tokens, groups, ambient, quotes ->
        SettingsUiState(
            mode = mode,
            hasNfcToken = tokens.any { it.kind == ShieldTokenKind.NFC },
            hasQrToken = tokens.any { it.kind == ShieldTokenKind.QR },
            blockedGroups = groups.map { BlockedGroupSummary(it.displayName, it.packageNames.size) },
            ambientSound = ambient,
            quotes = quotes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onSelectMode(mode: BlockingMode) {
        viewModelScope.launch { settingsRepository.setBlockingMode(mode) }
    }

    fun onToggleAmbientSound(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAmbientSoundEnabled(enabled) }
    }

    fun onToggleQuotes(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setQuotesEnabled(enabled) }
    }
}
