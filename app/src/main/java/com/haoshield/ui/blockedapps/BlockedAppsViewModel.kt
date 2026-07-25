package com.haoshield.ui.blockedapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.util.AppLabelProvider
import com.haoshield.data.util.InstalledAppsProvider
import com.haoshield.domain.repository.BlockingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlockedAppRow(
    val packageName: String,
    val label: String,
    val blocked: Boolean,
)

data class BlockedAppSection(
    val title: String,
    val rows: List<BlockedAppRow>,
)

data class BlockedAppsUiState(
    val sections: List<BlockedAppSection> = emptyList(),
    val totalBlocked: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class BlockedAppsViewModel @Inject constructor(
    private val blockingRepository: BlockingRepository,
    private val installedAppsProvider: InstalledAppsProvider,
    private val appLabelProvider: AppLabelProvider,
) : ViewModel() {

    private val presetGroups = blockingRepository.getPresetGroups()
    private val presetPackages = presetGroups.flatMap { it.packageNames }.toSet()

    /** Null until the first read completes, so we don't flash a misleading empty list. */
    private val installedPackages = MutableStateFlow<Set<String>?>(null)

    init {
        refresh()
    }

    /**
     * Re-read what's on the device. Called on every resume, so an app installed while this screen
     * was in the background appears when you come back.
     */
    fun refresh() {
        viewModelScope.launch {
            installedPackages.value = installedAppsProvider.getInstalledPackageNames()
        }
    }

    val uiState: StateFlow<BlockedAppsUiState> = combine(
        blockingRepository.observeBlockedPackageNames(),
        installedPackages,
    ) { blocked, installed ->
        if (installed == null) {
            BlockedAppsUiState(isLoading = true, totalBlocked = blocked.size)
        } else {
            buildState(blocked, installed)
        }
    }
        // Label lookups hit PackageManager; keep them off the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockedAppsUiState())

    fun onToggle(packageName: String, blocked: Boolean) {
        viewModelScope.launch {
            if (blocked) {
                blockingRepository.addBlockedPackage(packageName)
            } else {
                blockingRepository.removeBlockedPackage(packageName)
            }
        }
    }

    /**
     * Only apps actually on the device are listed — a list of apps you don't have is noise, and the
     * question is what pulls *you* away. Presets you haven't installed stay in the blocklist
     * regardless, so they're already covered if you install them later; they just aren't shown.
     */
    private fun buildState(blocked: Set<String>, installed: Set<String>): BlockedAppsUiState {
        val presetSections = presetGroups.mapNotNull { group ->
            val rows = group.packageNames
                .filter { it in installed }
                .map { pkg -> BlockedAppRow(pkg, appLabelProvider.getLabel(pkg), pkg in blocked) }
            if (rows.isEmpty()) null else BlockedAppSection(group.displayName, rows)
        }
        val custom = (blocked - presetPackages).filter { it in installed }.sorted()
        val customSection = if (custom.isEmpty()) {
            null
        } else {
            BlockedAppSection(
                title = "Added by you",
                rows = custom.map { pkg ->
                    BlockedAppRow(pkg, appLabelProvider.getLabel(pkg), blocked = true)
                },
            )
        }
        return BlockedAppsUiState(
            sections = presetSections + listOfNotNull(customSection),
            totalBlocked = blocked.size,
            isLoading = false,
        )
    }
}
