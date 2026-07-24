package com.haoshield.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.haoshield.domain.model.BlockingMode
import com.haoshield.domain.model.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings_preferences",
)

/**
 * Persisted user preferences: which blocking mode is active, and session defaults.
 */
@Singleton
class SettingsPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.settingsDataStore

    fun observeBlockingMode(): Flow<BlockingMode> =
        dataStore.data.map { preferences ->
            when (preferences[Keys.BLOCKING_MODE]) {
                BlockingMode.SHIELD.name -> BlockingMode.SHIELD
                else -> BlockingMode.SOFTWARE
            }
        }

    suspend fun setBlockingMode(mode: BlockingMode) {
        dataStore.edit { preferences ->
            preferences[Keys.BLOCKING_MODE] = mode.name
        }
    }

    fun observeAmbientSoundEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[Keys.AMBIENT_SOUND] ?: true
        }

    suspend fun setAmbientSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AMBIENT_SOUND] = enabled
        }
    }

    fun observeQuotesEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[Keys.QUOTES] ?: true
        }

    suspend fun setQuotesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.QUOTES] = enabled
        }
    }

    // --- Strict blocking (root) ---

    fun observeStrictBlockingEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[Keys.STRICT_BLOCKING] ?: false
        }

    suspend fun isStrictBlockingEnabled(): Boolean =
        dataStore.data.first()[Keys.STRICT_BLOCKING] ?: false

    suspend fun setStrictBlockingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.STRICT_BLOCKING] = enabled
        }
    }

    /**
     * Whether packages are currently OS-suspended. Persisted so that if the process is killed while
     * a strict session is live, we can still release the suspensions on next launch.
     */
    suspend fun isStrictApplied(): Boolean =
        dataStore.data.first()[Keys.STRICT_APPLIED] ?: false

    suspend fun setStrictApplied(applied: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.STRICT_APPLIED] = applied
        }
    }

    // --- Appearance ---

    fun observeThemePreference(): Flow<ThemePreference> =
        dataStore.data.map { preferences -> preferences.readTheme() }

    suspend fun getThemePreference(): ThemePreference =
        dataStore.data.first().readTheme()

    suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME] = preference.name
        }
    }

    private fun Preferences.readTheme(): ThemePreference = when (this[Keys.THEME]) {
        ThemePreference.LIGHT.name -> ThemePreference.LIGHT
        ThemePreference.DARK.name -> ThemePreference.DARK
        else -> ThemePreference.SYSTEM
    }

    // --- First-run intro ---

    fun observeHasSeenIntro(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[Keys.HAS_SEEN_INTRO] ?: false }

    suspend fun setHasSeenIntro(seen: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.HAS_SEEN_INTRO] = seen }
    }

    private object Keys {
        val BLOCKING_MODE = stringPreferencesKey("blocking_mode")
        val THEME = stringPreferencesKey("theme_preference")
        val HAS_SEEN_INTRO = booleanPreferencesKey("has_seen_intro")
        val AMBIENT_SOUND = booleanPreferencesKey("ambient_sound_enabled")
        val QUOTES = booleanPreferencesKey("quotes_enabled")
        val STRICT_BLOCKING = booleanPreferencesKey("strict_blocking_enabled")
        val STRICT_APPLIED = booleanPreferencesKey("strict_blocking_applied")
    }
}
