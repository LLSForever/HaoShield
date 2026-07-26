package com.haoshield.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.haoshield.domain.service.ShieldPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.shieldDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shield_preferences",
)

@Singleton
class ShieldPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShieldPreferences {
    private val dataStore = context.shieldDataStore

    // --- Registered NFC UID (unchanged key — preserves existing registrations) ---

    override fun observeRegisteredUid(): Flow<String?> =
        dataStore.data.map { preferences ->
            preferences[Keys.REGISTERED_UID]
        }

    override suspend fun getRegisteredUid(): String? =
        dataStore.data.first()[Keys.REGISTERED_UID]

    override suspend fun persistRegisteredUid(uid: String) {
        dataStore.edit { preferences ->
            preferences[Keys.REGISTERED_UID] = uid
        }
    }

    override suspend fun clearRegisteredUid() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.REGISTERED_UID)
        }
    }

    // --- Registered QR payload ---

    override fun observeRegisteredQr(): Flow<String?> =
        dataStore.data.map { preferences ->
            preferences[Keys.REGISTERED_QR]
        }

    override suspend fun getRegisteredQr(): String? =
        dataStore.data.first()[Keys.REGISTERED_QR]

    override suspend fun persistRegisteredQr(payload: String) {
        dataStore.edit { preferences ->
            preferences[Keys.REGISTERED_QR] = payload
        }
    }

    override suspend fun clearRegisteredQr() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.REGISTERED_QR)
        }
    }

    // --- Pending QR payload (generated but not yet confirmed by a scan) ---

    override suspend fun getPendingQr(): String? =
        dataStore.data.first()[Keys.PENDING_QR]

    override suspend fun persistPendingQr(payload: String) {
        dataStore.edit { preferences ->
            preferences[Keys.PENDING_QR] = payload
        }
    }

    override suspend fun clearPendingQr() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.PENDING_QR)
        }
    }

    private object Keys {
        val REGISTERED_UID = stringPreferencesKey("registered_shield_uid")
        val REGISTERED_QR = stringPreferencesKey("registered_qr_token")
        val PENDING_QR = stringPreferencesKey("pending_qr_token")
    }
}
