package com.haoshield.data.shield

import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.service.SessionManager
import com.haoshield.domain.service.ShieldScanHandler
import com.haoshield.domain.service.ShieldTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShieldScanHandlerImpl @Inject constructor(
    private val shieldTokenStore: ShieldTokenStore,
    private val sessionManager: SessionManager,
) : ShieldScanHandler {

    private val scanResults = MutableSharedFlow<ShieldScanResult>(extraBufferCapacity = 1)
    private var lastHandledId: String? = null
    private var lastHandledMode: ScanMode? = null
    private var lastHandledAtMillis: Long = 0L

    override fun observeScanResults(): Flow<ShieldScanResult> = scanResults.asSharedFlow()

    override suspend fun handleScan(token: ShieldToken, mode: ScanMode): ShieldScanResult {
        val normalized = token.normalized()
        // A physical tap or a continuously-decoding camera can fire the same token repeatedly;
        // ignore rapid repeats silently (no emit) so listeners aren't spammed. Keyed on (id, mode)
        // and a short window so a legitimate register→start or start→end tap isn't suppressed.
        if (shouldDebounce(normalized.id, mode)) {
            return ShieldScanResult.Failed("Scan ignored — try again in a moment.")
        }
        recordHandledScan(normalized.id, mode)

        val result = when (mode) {
            ScanMode.REGISTRATION -> handleRegistration(normalized)
            ScanMode.SESSION -> handleSession(normalized)
        }
        scanResults.emit(result)
        return result
    }

    private suspend fun handleRegistration(token: ShieldToken): ShieldScanResult {
        // QR registration is confirmed against the code the app just generated, proving the
        // physical print actually scans. NFC tags register directly on first tap.
        if (token.kind == ShieldTokenKind.QR) {
            val pending = shieldTokenStore.getPendingQrPayload()
            when {
                pending == null ->
                    return ShieldScanResult.Failed("Generate a Shield code first, then scan it.")
                pending != token.id ->
                    return ShieldScanResult.Failed("This isn't the code you just created.")
            }
        }
        return shieldTokenStore.registerToken(token).fold(
            onSuccess = {
                if (token.kind == ShieldTokenKind.QR) {
                    shieldTokenStore.clearPendingQrPayload()
                }
                ShieldScanResult.RegistrationComplete(token)
            },
            onFailure = { ShieldScanResult.Failed(it.message ?: "Unable to register shield.") },
        )
    }

    private suspend fun handleSession(token: ShieldToken): ShieldScanResult {
        if (!shieldTokenStore.hasAnyRegisteredToken()) {
            return ShieldScanResult.NoShieldRegistered
        }
        if (!shieldTokenStore.validate(token)) {
            return ShieldScanResult.InvalidShield(token)
        }

        val activeSession = sessionManager.getActiveSession()
        return when {
            activeSession == null -> {
                val session = sessionManager.startSession(SessionMode.SHIELD)
                ShieldScanResult.SessionStarted(session)
            }
            activeSession.mode == SessionMode.SHIELD -> {
                when (val result = sessionManager.endSession(SessionEndMethod.SHIELD_SCAN, token)) {
                    is SessionEndResult.Ended -> ShieldScanResult.SessionEnded(result.session)
                    is SessionEndResult.RequiresShieldScan ->
                        ShieldScanResult.Failed(result.message)
                    is SessionEndResult.Failed -> ShieldScanResult.Failed(result.reason)
                }
            }
            else -> ShieldScanResult.SoftwareSessionActive
        }
    }

    private fun shouldDebounce(id: String, mode: ScanMode): Boolean {
        val now = System.currentTimeMillis()
        return id == lastHandledId &&
            mode == lastHandledMode &&
            now - lastHandledAtMillis < DEBOUNCE_MILLIS
    }

    private fun recordHandledScan(id: String, mode: ScanMode) {
        lastHandledId = id
        lastHandledMode = mode
        lastHandledAtMillis = System.currentTimeMillis()
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 1_200L
    }
}
