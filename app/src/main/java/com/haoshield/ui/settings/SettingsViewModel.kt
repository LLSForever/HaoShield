package com.haoshield.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.blocking.StrictBlockingController
import com.haoshield.data.root.RootShell
import com.haoshield.domain.model.BlockingMode
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.model.ThemePreference
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
    val strictBlocking: Boolean = false,
    val rootAvailable: Boolean = false,
    val theme: ThemePreference = ThemePreference.SYSTEM,
) {
    val hasAnyToken: Boolean get() = hasNfcToken || hasQrToken
}

private data class SessionToggles(
    val ambient: Boolean,
    val quotes: Boolean,
    val strict: Boolean,
    val theme: ThemePreference,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val strictBlockingController: StrictBlockingController,
    rootShell: RootShell,
    shieldTokenStore: ShieldTokenStore,
    blockingRepository: BlockingRepository,
) : ViewModel() {

    // Passive check (no su invocation, no root prompt) — just whether Strict mode can be offered.
    private val rootAvailable: Boolean = rootShell.isRootBinaryPresent()

    private val sessionToggles = combine(
        settingsRepository.observeAmbientSoundEnabled(),
        settingsRepository.observeQuotesEnabled(),
        settingsRepository.observeStrictBlockingEnabled(),
        settingsRepository.observeThemePreference(),
    ) { ambient, quotes, strict, theme -> SessionToggles(ambient, quotes, strict, theme) }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeBlockingMode(),
        shieldTokenStore.observeRegisteredTokens(),
        blockingRepository.observeBlockedGroups(),
        sessionToggles,
    ) { mode, tokens, groups, toggles ->
        SettingsUiState(
            mode = mode,
            hasNfcToken = tokens.any { it.kind == ShieldTokenKind.NFC },
            hasQrToken = tokens.any { it.kind == ShieldTokenKind.QR },
            blockedGroups = groups.map { BlockedGroupSummary(it.displayName, it.packageNames.size) },
            ambientSound = toggles.ambient,
            quotes = toggles.quotes,
            strictBlocking = toggles.strict,
            rootAvailable = rootAvailable,
            theme = toggles.theme,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(rootAvailable = rootAvailable),
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

    fun onSelectTheme(preference: ThemePreference) {
        viewModelScope.launch { settingsRepository.setThemePreference(preference) }
    }

    fun onToggleStrictBlocking(enabled: Boolean) {
        if (enabled && !rootAvailable) return
        viewModelScope.launch {
            settingsRepository.setStrictBlockingEnabled(enabled)
            // If turned off while apps are suspended, release them immediately rather than waiting
            // for the session to end.
            if (!enabled) {
                strictBlockingController.releaseAll()
            }
        }
    }
}
