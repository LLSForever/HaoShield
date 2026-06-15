package com.haoshield.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.util.AppLabelProvider
import com.haoshield.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JournalViewModel @Inject constructor(
    journalRepository: JournalRepository,
    private val appLabelProvider: AppLabelProvider,
) : ViewModel() {

    val entries: StateFlow<List<JournalEntryUiModel>> =
        journalRepository.observeEntries()
            .map { journalEntries ->
                journalEntries.map { entry ->
                    val label = entry.unblockedPackageName?.let(appLabelProvider::getLabel)
                    entry.toUiModel(appLabel = label)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
}