package com.haoshield.domain.service

import android.app.Activity
import com.haoshield.domain.model.ScanMode

/**
 * Hardware NFC concerns only: availability and foreground reader-mode lifecycle. Discovered
 * tags are handed to [ShieldScanHandler]; registration/validation live in [ShieldTokenStore].
 */
interface NfcManager {
    fun isNfcAvailable(): Boolean

    fun isNfcEnabled(): Boolean

    fun enableForegroundReader(
        activity: Activity,
        mode: ScanMode = ScanMode.SESSION,
    )

    fun disableForegroundReader(activity: Activity)
}
