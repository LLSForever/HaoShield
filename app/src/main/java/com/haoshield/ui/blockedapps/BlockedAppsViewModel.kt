package com.haoshield.ui.blockedapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.util.AppLabelProvider
import com.haoshield.domain.repository.BlockingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
)

@HiltViewModel
class BlockedAppsViewModel @Inject constructor(
    private val blockingRepository: BlockingRepository,
    private val appLabelProvider: AppLabelProvider,
) : ViewModel() {

    private val presetGroups = blockingRepository.getPresetGroups()
    private val presetPackages = presetGroups.flatMap { it.packageNames }.toSet()

    val uiState: StateFlow<BlockedAppsUiState> =
        blockingRepository.observeBlockedPackageNames()
            .map { blocked -> buildState(blocked) }
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

    private fun buildState(blocked: Set<String>): BlockedAppsUiState {
        val presetSections = presetGroups.map { group ->
            BlockedAppSection(
                title = group.displayName,
                rows = group.packageNames.map { pkg ->
                    BlockedAppRow(pkg, appLabelProvider.getLabel(pkg), pkg in blocked)
                },
            )
        }
        val custom = (blocked - presetPackages).sorted()
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
        )
    }
}
