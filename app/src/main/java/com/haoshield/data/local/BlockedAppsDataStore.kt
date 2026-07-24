package com.haoshield.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.blockedAppsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "blocked_apps_preferences",
)

/**
 * The user's chosen blocklist. Emits null until the user has customised anything, so callers can
 * fall back to the curated presets. An explicitly empty set (the user removed everything) is
 * distinct from null and is honoured.
 */
@Singleton
class BlockedAppsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.blockedAppsDataStore

    fun observeBlockedPackages(): Flow<Set<String>?> =
        dataStore.data.map { preferences ->
            if (preferences[Keys.CUSTOMISED] == true) {
                preferences[Keys.PACKAGES].orEmpty()
            } else {
                null
            }
        }

    suspend fun setBlockedPackages(packages: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.CUSTOMISED] = true
            preferences[Keys.PACKAGES] = packages
        }
    }

    private object Keys {
        val CUSTOMISED = booleanPreferencesKey("user_customised")
        val PACKAGES = stringSetPreferencesKey("blocked_packages")
    }
}
