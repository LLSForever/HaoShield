package com.haoshield.domain.service

import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import kotlinx.coroutines.flow.Flow

/**
 * Registration and validation of the user's Hǎo Shield tokens. At most one token per kind
 * (one NFC tag and/or one printed QR code). Depends only on storage — never on session state —
 * so it can be safely used by both [SessionManager] and [ShieldScanHandler] without a DI cycle.
 */
interface ShieldTokenStore {
    fun observeRegisteredTokens(): Flow<List<ShieldToken>>

    suspend fun getRegisteredTokens(): List<ShieldToken>

    suspend fun hasAnyRegisteredToken(): Boolean

    suspend fun registerToken(token: ShieldToken): Result<Unit>

    suspend fun clearToken(kind: ShieldTokenKind): Result<Unit>

    /** True if [token] matches the registered token of its kind. */
    suspend fun validate(token: ShieldToken): Boolean

    // --- Pending QR registration (generated but not yet confirmed by a scan) ---

    suspend fun setPendingQrPayload(payload: String)

    suspend fun getPendingQrPayload(): String?

    suspend fun clearPendingQrPayload()
}
