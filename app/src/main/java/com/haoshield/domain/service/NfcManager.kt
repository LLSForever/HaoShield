package com.haoshield.domain.service

import android.app.Activity
import com.haoshield.domain.model.NfcReaderMode
import com.haoshield.domain.model.NfcTapResult
import kotlinx.coroutines.flow.Flow

interface NfcManager {
    fun observeRegisteredShieldUid(): Flow<String?>

    suspend fun getRegisteredShieldUid(): String?

    fun isNfcAvailable(): Boolean

    fun isNfcEnabled(): Boolean

    suspend fun registerShield(uid: String): Result<Unit>

    suspend fun clearRegisteredShield(): Result<Unit>

    suspend fun validateShield(uid: String): Boolean

    fun observeTapResults(): Flow<NfcTapResult>

    fun enableForegroundReader(
        activity: Activity,
        mode: NfcReaderMode = NfcReaderMode.SESSION,
    )

    fun disableForegroundReader(activity: Activity)
}