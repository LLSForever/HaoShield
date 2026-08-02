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

    /**
     * Leave registration mode and return the reader to ordinary SESSION scanning. Until this is
     * called, a [enableForegroundReader] request for SESSION (e.g. from the host activity's
     * onResume) is treated as REGISTRATION, so returning from system settings mid-registration
     * doesn't silently downgrade the reader and break tag registration.
     */
    fun exitRegistrationMode(activity: Activity)
}
