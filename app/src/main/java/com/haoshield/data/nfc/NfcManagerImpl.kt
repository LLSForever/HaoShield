package com.haoshield.data.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import com.haoshield.data.local.ShieldPreferencesDataStore
import com.haoshield.di.ApplicationScope
import com.haoshield.domain.model.NfcReaderMode
import com.haoshield.domain.model.NfcTapResult
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionEndResult
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.service.NfcManager
import com.haoshield.domain.service.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shieldPreferencesDataStore: ShieldPreferencesDataStore,
    private val sessionManager: SessionManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : NfcManager {

    private val tapResults = MutableSharedFlow<NfcTapResult>(extraBufferCapacity = 1)
    private var readerMode: NfcReaderMode = NfcReaderMode.SESSION
    private var boundActivity: WeakReference<Activity>? = null
    private var lastHandledUid: String? = null
    private var lastHandledAtMillis: Long = 0L

    override fun observeRegisteredShieldUid(): Flow<String?> =
        shieldPreferencesDataStore.observeRegisteredUid()

    override suspend fun getRegisteredShieldUid(): String? =
        shieldPreferencesDataStore.getRegisteredUid()

    override fun isNfcAvailable(): Boolean =
        NfcAdapter.getDefaultAdapter(context) != null

    override fun isNfcEnabled(): Boolean =
        NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    override suspend fun registerShield(uid: String): Result<Unit> = runCatching {
        require(uid.isNotBlank()) { "Shield UID cannot be blank." }
        shieldPreferencesDataStore.persistRegisteredUid(uid.normalizeShieldUid())
    }

    override suspend fun clearRegisteredShield(): Result<Unit> = runCatching {
        shieldPreferencesDataStore.clearRegisteredUid()
    }

    override suspend fun validateShield(uid: String): Boolean {
        val registered = getRegisteredShieldUid() ?: return false
        return registered.equals(uid.normalizeShieldUid(), ignoreCase = true)
    }

    override fun observeTapResults(): Flow<NfcTapResult> = tapResults.asSharedFlow()

    override fun enableForegroundReader(activity: Activity, mode: NfcReaderMode) {
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
        applicationScope.launch {
            val result = handleTagDiscovered(tag)
            tapResults.emit(result)
        }
    }

    private suspend fun handleTagDiscovered(tag: Tag): NfcTapResult {
        val uid = tag.toShieldUid()
        if (shouldDebounce(uid)) {
            return NfcTapResult.Failed("Tap ignored — try again in a moment.")
        }
        recordHandledTap(uid)

        return when (readerMode) {
            NfcReaderMode.REGISTRATION -> handleRegistrationTap(uid)
            NfcReaderMode.SESSION -> handleSessionTap(uid)
        }
    }

    private suspend fun handleRegistrationTap(uid: String): NfcTapResult {
        return registerShield(uid)
            .fold(
                onSuccess = { NfcTapResult.RegistrationComplete(uid.normalizeShieldUid()) },
                onFailure = { NfcTapResult.Failed(it.message ?: "Unable to register shield.") },
            )
    }

    private suspend fun handleSessionTap(uid: String): NfcTapResult {
        val registeredUid = getRegisteredShieldUid()
            ?: return NfcTapResult.NoShieldRegistered

        if (!registeredUid.equals(uid.normalizeShieldUid(), ignoreCase = true)) {
            return NfcTapResult.InvalidShield(uid)
        }

        val activeSession = sessionManager.getActiveSession()
        return when {
            activeSession == null -> {
                val session = sessionManager.startSession(SessionMode.SHIELD)
                NfcTapResult.SessionStarted(session)
            }
            activeSession.mode == SessionMode.SHIELD -> {
                when (val result = sessionManager.endSession(SessionEndMethod.SHIELD_SCAN, uid)) {
                    is SessionEndResult.Ended -> NfcTapResult.SessionEnded(result.session)
                    is SessionEndResult.RequiresShieldScan ->
                        NfcTapResult.Failed(result.message)
                    is SessionEndResult.Failed ->
                        NfcTapResult.Failed(result.reason)
                }
            }
            else -> NfcTapResult.SoftwareSessionActive
        }
    }

    private fun shouldDebounce(uid: String): Boolean {
        val now = System.currentTimeMillis()
        return uid == lastHandledUid && now - lastHandledAtMillis < DEBOUNCE_MILLIS
    }

    private fun recordHandledTap(uid: String) {
        lastHandledUid = uid
        lastHandledAtMillis = System.currentTimeMillis()
    }

    private fun String.normalizeShieldUid(): String = uppercase()

    private companion object {
        const val DEBOUNCE_MILLIS = 2_000L
    }
}