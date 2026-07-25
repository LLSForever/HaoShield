package com.haoshield.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.data.util.AppLabelProvider
import com.haoshield.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JournalViewModel @Inject constructor(
    journalRepository: JournalRepository,
    private val appLabelProvider: AppLabelProvider,
) : ViewModel() {

    val sessions: StateFlow<List<JournalSessionGroup>> =
        journalRepository.observeEntries()
            .map { entries ->
                entries
                    // Group a sitting together by its session; entries with no session id (should
                    // not happen in practice) each stand alone via a unique fallback key.
                    .groupBy { it.sessionId?.let { id -> "s$id" } ?: "e${it.id}" }
                    .map { (key, groupEntries) ->
                        val sorted = groupEntries.sortedBy { it.createdAtEpochMillis }
                        JournalSessionGroup(
                            key = key,
                            dateLabel = JournalDateFormatter.formatDay(
                                sorted.first().createdAtEpochMillis,
                            ),
                            latestMillis = sorted.last().createdAtEpochMillis,
                            entries = sorted.map { entry ->
                                val label = entry.unblockedPackageName?.let(appLabelProvider::getLabel)
                                entry.toUiModel(appLabel = label)
                            },
                        )
                    }
                    .sortedByDescending { it.latestMillis }
            }
            // Label lookups hit PackageManager; keep them off the main thread.
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
}
