package com.haoshield.data.service

import com.haoshield.data.local.ShieldPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcShieldServiceImpl @Inject constructor(
    private val shieldPreferencesDataStore: ShieldPreferencesDataStore,
) : NfcShieldService {

    override fun observeRegisteredShieldId(): Flow<String?> =
        shieldPreferencesDataStore.observeRegisteredUid()

    override suspend fun getRegisteredShieldId(): String? =
        shieldPreferencesDataStore.getRegisteredUid()

    override suspend fun registerShield(tagId: String): Result<Unit> = runCatching {
        require(tagId.isNotBlank()) { "Shield UID cannot be blank." }
        shieldPreferencesDataStore.persistRegisteredUid(tagId.uppercase())
    }

    override suspend fun validateShield(tagId: String): Boolean {
        val registered = getRegisteredShieldId() ?: return false
        return registered.equals(tagId.uppercase(), ignoreCase = true)
    }
}