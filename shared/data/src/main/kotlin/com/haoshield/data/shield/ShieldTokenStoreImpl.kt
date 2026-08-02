package com.haoshield.data.shield

import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.service.ShieldPreferences
import com.haoshield.domain.service.ShieldTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShieldTokenStoreImpl @Inject constructor(
    private val shieldPreferencesDataStore: ShieldPreferences,
) : ShieldTokenStore {

    override fun observeRegisteredTokens(): Flow<List<ShieldToken>> =
        combine(
            shieldPreferencesDataStore.observeRegisteredUid(),
            shieldPreferencesDataStore.observeRegisteredQr(),
        ) { uid, qr ->
            buildList {
                uid?.let { add(ShieldToken(ShieldTokenKind.NFC, it)) }
                qr?.let { add(ShieldToken(ShieldTokenKind.QR, it)) }
            }
        }

    override suspend fun getRegisteredTokens(): List<ShieldToken> = buildList {
        shieldPreferencesDataStore.getRegisteredUid()?.let {
            add(ShieldToken(ShieldTokenKind.NFC, it))
        }
        shieldPreferencesDataStore.getRegisteredQr()?.let {
            add(ShieldToken(ShieldTokenKind.QR, it))
        }
    }

    override suspend fun hasAnyRegisteredToken(): Boolean =
        getRegisteredTokens().isNotEmpty()

    override suspend fun registerToken(token: ShieldToken): Result<Unit> = runCatching {
        require(token.id.isNotBlank()) { "Shield token cannot be blank." }
        val normalized = token.normalized()
        when (normalized.kind) {
            ShieldTokenKind.NFC -> shieldPreferencesDataStore.persistRegisteredUid(normalized.id)
            ShieldTokenKind.QR -> shieldPreferencesDataStore.persistRegisteredQr(normalized.id)
        }
    }

    override suspend fun clearToken(kind: ShieldTokenKind): Result<Unit> = runCatching {
        when (kind) {
            ShieldTokenKind.NFC -> shieldPreferencesDataStore.clearRegisteredUid()
            ShieldTokenKind.QR -> shieldPreferencesDataStore.clearRegisteredQr()
        }
    }

    override suspend fun validate(token: ShieldToken): Boolean {
        val normalized = token.normalized()
        val registered = when (normalized.kind) {
            ShieldTokenKind.NFC -> shieldPreferencesDataStore.getRegisteredUid()
            ShieldTokenKind.QR -> shieldPreferencesDataStore.getRegisteredQr()
        } ?: return false
        return registered == normalized.id
    }

    override suspend fun setPendingQrPayload(payload: String) =
        shieldPreferencesDataStore.persistPendingQr(payload)

    override suspend fun getPendingQrPayload(): String? =
        shieldPreferencesDataStore.getPendingQr()

    override suspend fun clearPendingQrPayload() =
        shieldPreferencesDataStore.clearPendingQr()
}
