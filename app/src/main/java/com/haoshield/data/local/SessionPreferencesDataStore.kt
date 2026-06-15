package com.haoshield.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "session_preferences",
)

data class PersistedSessionSnapshot(
    val session: Session,
    val temporarilyAllowedPackages: Set<String>,
)

@Singleton
class SessionPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.sessionDataStore

    fun observePersistedSession(): Flow<PersistedSessionSnapshot?> =
        dataStore.data.map { preferences ->
            val isActive = preferences[Keys.IS_ACTIVE] ?: false
            if (!isActive) return@map null

            val modeName = preferences[Keys.MODE] ?: return@map null
            val mode = runCatching { SessionMode.valueOf(modeName) }.getOrNull()
                ?: return@map null

            PersistedSessionSnapshot(
                session = Session(
                    id = preferences[Keys.SESSION_ID] ?: return@map null,
                    mode = mode,
                    startedAtEpochMillis = preferences[Keys.STARTED_AT] ?: return@map null,
                    isActive = true,
                ),
                temporarilyAllowedPackages = preferences[Keys.ALLOWED_PACKAGES].orEmpty(),
            )
        }

    suspend fun persistActiveSession(
        session: Session,
        temporarilyAllowedPackages: Set<String>,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_ACTIVE] = true
            preferences[Keys.SESSION_ID] = session.id
            preferences[Keys.MODE] = session.mode.name
            preferences[Keys.STARTED_AT] = session.startedAtEpochMillis
            preferences[Keys.ALLOWED_PACKAGES] = temporarilyAllowedPackages
        }
    }

    suspend fun persistAllowedPackages(packages: Set<String>) {
        dataStore.edit { preferences ->
            if (preferences[Keys.IS_ACTIVE] == true) {
                preferences[Keys.ALLOWED_PACKAGES] = packages
            }
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.IS_ACTIVE)
            preferences.remove(Keys.SESSION_ID)
            preferences.remove(Keys.MODE)
            preferences.remove(Keys.STARTED_AT)
            preferences.remove(Keys.ALLOWED_PACKAGES)
        }
    }

    private object Keys {
        val IS_ACTIVE = booleanPreferencesKey("session_is_active")
        val SESSION_ID = longPreferencesKey("session_id")
        val MODE = stringPreferencesKey("session_mode")
        val STARTED_AT = longPreferencesKey("session_started_at")
        val ALLOWED_PACKAGES = stringSetPreferencesKey("allowed_packages")
    }
}