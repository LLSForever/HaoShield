package com.haoshield.domain.service

import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Orchestrates what happens when a shield token is scanned — whether via an NFC tap or a
 * QR camera scan. Registration and session start/end logic lives here (once), so both input
 * methods share identical behavior. Results are emitted on [observeScanResults] for any screen
 * to react to, and also returned to the immediate caller.
 */
interface ShieldScanHandler {
    fun observeScanResults(): Flow<ShieldScanResult>

    suspend fun handleScan(token: ShieldToken, mode: ScanMode): ShieldScanResult
}
