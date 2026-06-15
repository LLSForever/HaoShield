package com.haoshield.data.service

import kotlinx.coroutines.flow.Flow

/**
 * NFC registration and validation for the physical Hǎo Shield. Implementation in a later phase.
 */
interface NfcShieldService {
    fun observeRegisteredShieldId(): Flow<String?>

    suspend fun getRegisteredShieldId(): String?

    suspend fun registerShield(tagId: String): Result<Unit>

    suspend fun validateShield(tagId: String): Boolean
}