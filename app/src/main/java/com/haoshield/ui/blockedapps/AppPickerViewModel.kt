package com.haoshield.ui.blockedapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.util.InstalledApp
import com.haoshield.data.util.InstalledAppsProvider
import com.haoshield.domain.repository.BlockingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppPickerUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
)

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val installedAppsProvider: InstalledAppsProvider,
    private val blockingRepository: BlockingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPickerUiState())
    val uiState: StateFlow<AppPickerUiState> = _uiState.asStateFlow()

    private val _added = Channel<Unit>(Channel.BUFFERED)
    /** Emits after an app is added, so the screen can return. */
    val addedEvents = _added.receiveAsFlow()

    init {
        viewModelScope.launch {
            val alreadyBlocked = blockingRepository.observeBlockedPackageNames().first()
            val apps = installedAppsProvider.getLaunchableApps()
                .filterNot { it.packageName in alreadyBlocked }
            _uiState.update { it.copy(isLoading = false, apps = apps) }
        }
    }

    fun onAdd(packageName: String) {
        viewModelScope.launch {
            blockingRepository.addBlockedPackage(packageName)
            _added.send(Unit)
        }
    }
}
