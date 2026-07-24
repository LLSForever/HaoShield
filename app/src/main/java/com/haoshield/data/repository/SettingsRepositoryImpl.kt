package com.haoshield.data.repository

import com.haoshield.data.local.SettingsPreferencesDataStore
import com.haoshield.domain.model.BlockingMode
import com.haoshield.domain.model.ThemePreference
import com.haoshield.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsPreferencesDataStore: SettingsPreferencesDataStore,
) : SettingsRepository {

    override fun observeBlockingMode(): Flow<BlockingMode> =
        settingsPreferencesDataStore.observeBlockingMode()

    override suspend fun setBlockingMode(mode: BlockingMode) =
        settingsPreferencesDataStore.setBlockingMode(mode)

    override fun observeAmbientSoundEnabled(): Flow<Boolean> =
        settingsPreferencesDataStore.observeAmbientSoundEnabled()

    override suspend fun setAmbientSoundEnabled(enabled: Boolean) =
        settingsPreferencesDataStore.setAmbientSoundEnabled(enabled)

    override fun observeQuotesEnabled(): Flow<Boolean> =
        settingsPreferencesDataStore.observeQuotesEnabled()

    override suspend fun setQuotesEnabled(enabled: Boolean) =
        settingsPreferencesDataStore.setQuotesEnabled(enabled)

    override fun observeStrictBlockingEnabled(): Flow<Boolean> =
        settingsPreferencesDataStore.observeStrictBlockingEnabled()

    override suspend fun setStrictBlockingEnabled(enabled: Boolean) =
        settingsPreferencesDataStore.setStrictBlockingEnabled(enabled)

    override fun observeThemePreference(): Flow<ThemePreference> =
        settingsPreferencesDataStore.observeThemePreference()

    override suspend fun setThemePreference(preference: ThemePreference) =
        settingsPreferencesDataStore.setThemePreference(preference)

    override fun observeHasSeenIntro(): Flow<Boolean> =
        settingsPreferencesDataStore.observeHasSeenIntro()

    override suspend fun setHasSeenIntro(seen: Boolean) =
        settingsPreferencesDataStore.setHasSeenIntro(seen)
}
