package com.haoshield.data.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.haoshield.di.ApplicationScope
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.service.NfcManager
import com.haoshield.domain.service.ShieldScanHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanHandler: ShieldScanHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : NfcManager {

    private var readerMode: ScanMode = ScanMode.SESSION
    private var boundActivity: WeakReference<Activity>? = null

    // While registering a Shield, SESSION requests (e.g. the host activity's onResume, which fires
    // when the user returns from enabling NFC in system settings) must not downgrade the reader.
    private var registrationActive: Boolean = false

    override fun isNfcAvailable(): Boolean =
        NfcAdapter.getDefaultAdapter(context) != null

    override fun isNfcEnabled(): Boolean =
        NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    override fun enableForegroundReader(activity: Activity, mode: ScanMode) {
        if (mode == ScanMode.REGISTRATION) {
            registrationActive = true
        }
        // A SESSION request while registration is still active keeps REGISTRATION semantics.
        readerMode = if (registrationActive) ScanMode.REGISTRATION else mode
        boundActivity = WeakReference(activity)

        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return

        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        adapter.enableReaderMode(
            activity,
            { tag -> onTagDiscovered(tag) },
            flags,
            null,
        )
    }

    override fun disableForegroundReader(activity: Activity) {
        // Only stops the hardware reader (e.g. onPause). The registration intent survives the pause
        // so returning to the app re-arms REGISTRATION, not SESSION.
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
        if (boundActivity?.get() == activity) {
            boundActivity = null
        }
    }

    override fun exitRegistrationMode(activity: Activity) {
        registrationActive = false
        enableForegroundReader(activity, ScanMode.SESSION)
    }

    private fun onTagDiscovered(tag: Tag) {
        val token = ShieldToken(ShieldTokenKind.NFC, tag.toShieldUid())
        applicationScope.launch {
            scanHandler.handleScan(token, readerMode)
        }
    }
}
