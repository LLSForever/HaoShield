package com.haoshield.ui.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haoshield.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Marks the intro as seen so it doesn't show again on next launch. */
    fun onComplete() {
        viewModelScope.launch { settingsRepository.setHasSeenIntro(true) }
    }
}
