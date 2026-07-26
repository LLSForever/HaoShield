package com.haoshield.domain.service

import kotlinx.coroutines.flow.Flow

/**
 * Where a registered Shield is written down, as raw payloads.
 *
 * The two kinds are stored under their own keys rather than as one list, because the NFC key
 * predates QR and existing registrations must keep working across the update.
 */
interface ShieldPreferences {
    fun observeRegisteredUid(): Flow<String?>

    suspend fun getRegisteredUid(): String?

    suspend fun persistRegisteredUid(uid: String)

    suspend fun clearRegisteredUid()

    fun observeRegisteredQr(): Flow<String?>

    suspend fun getRegisteredQr(): String?

    suspend fun persistRegisteredQr(payload: String)

    suspend fun clearRegisteredQr()

    /** A QR generated but not yet confirmed by a scan, so an unscanned code never counts. */
    suspend fun getPendingQr(): String?

    suspend fun persistPendingQr(payload: String)

    suspend fun clearPendingQr()
}
