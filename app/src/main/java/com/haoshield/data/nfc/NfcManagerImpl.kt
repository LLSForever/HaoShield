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

    override fun isNfcAvailable(): Boolean =
        NfcAdapter.getDefaultAdapter(context) != null

    override fun isNfcEnabled(): Boolean =
        NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    override fun enableForegroundReader(activity: Activity, mode: ScanMode) {
        readerMode = mode
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
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
        if (boundActivity?.get() == activity) {
            boundActivity = null
        }
    }

    private fun onTagDiscovered(tag: Tag) {
        val token = ShieldToken(ShieldTokenKind.NFC, tag.toShieldUid())
        applicationScope.launch {
            scanHandler.handleScan(token, readerMode)
        }
    }
}
