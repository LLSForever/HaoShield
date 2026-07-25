package com.haoshield

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import com.haoshield.data.qr.ShieldQr
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.domain.model.ThemePreference
import com.haoshield.domain.repository.SettingsRepository
import com.haoshield.domain.service.NfcManager
import com.haoshield.domain.service.ShieldScanHandler
import com.haoshield.ui.navigation.HaoShieldNavHost
import com.haoshield.ui.navigation.Route
import com.haoshield.ui.theme.HaoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var nfcManager: NfcManager

    @Inject lateinit var shieldScanHandler: ShieldScanHandler

    @Inject lateinit var settingsRepository: SettingsRepository

    // Set when the blocking overlay asks us to open the unblock screen for a specific app.
    private val pendingUnblockPackage = mutableStateOf<String?>(null)

    // Set when the blocking overlay asks to return to the running session.
    private val pendingOpenSession = mutableStateOf(false)

    // Set when a printed Shield was scanned by an outside camera app and opened us via the link.
    private val pendingShieldPayload = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingUnblockPackage.value = intent?.getStringExtra(EXTRA_UNBLOCK_PACKAGE)
        pendingOpenSession.value = intent?.getBooleanExtra(EXTRA_OPEN_SESSION, false) == true
        pendingShieldPayload.value = shieldPayloadFrom(intent)
        // Consume them so a later configuration-change recreate doesn't re-navigate or re-scan.
        intent?.removeExtra(EXTRA_UNBLOCK_PACKAGE)
        intent?.removeExtra(EXTRA_OPEN_SESSION)
        intent?.data = null
        setContent {
            val themePreference by settingsRepository.observeThemePreference()
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val dark = when (themePreference) {
                ThemePreference.SYSTEM -> systemDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            // Keep the system bar icons legible against whichever ground is showing.
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            HaoTheme(dark = dark) {
                // Resolve the first-run flag before choosing a start destination. Until it loads we
                // render nothing but the paper ground — indistinguishable from the launch frame.
                val hasSeenIntro by produceState<Boolean?>(initialValue = null) {
                    value = settingsRepository.observeHasSeenIntro().first()
                }

                hasSeenIntro?.let { seen ->
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                Box(modifier = Modifier.fillMaxSize()) {
                    HaoShieldNavHost(
                        navController = navController,
                        startDestination = if (seen) Route.Home.path else Route.Intro.path,
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                    )
                }

                // Declared before the scan below, so the collector is subscribed by the time a
                // deep-linked scan emits — the result flow has no replay.
                // A shield tap/scan can happen on any screen, so react to results here rather
                // than in a single screen. Navigation on start/end; a quiet snackbar otherwise.
                LaunchedEffect(Unit) {
                    shieldScanHandler.observeScanResults().collect { result ->
                        when (result) {
                            is ShieldScanResult.SessionStarted -> {
                                navController.navigate(Route.Protected.path) {
                                    popUpTo(Route.Home.path) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            is ShieldScanResult.SessionEnded -> {
                                // Ending offers a brief reflection, which then returns Home.
                                navController.navigate(Route.Reflection.path) {
                                    popUpTo(Route.Home.path) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            is ShieldScanResult.InvalidShield ->
                                snackbarHostState.showSnackbar("That isn't your registered Hǎo Shield.")
                            ShieldScanResult.NoShieldRegistered ->
                                snackbarHostState.showSnackbar("No Hǎo Shield is registered yet.")
                            ShieldScanResult.SoftwareSessionActive ->
                                snackbarHostState.showSnackbar("A Software session is already running.")
                            is ShieldScanResult.Failed ->
                                snackbarHostState.showSnackbar(result.reason)
                            is ShieldScanResult.RegistrationComplete -> Unit
                        }
                    }
                }

                val unblockPackage = pendingUnblockPackage.value
                LaunchedEffect(unblockPackage) {
                    if (unblockPackage != null) {
                        navController.navigate(Route.Unblock.createRoute(unblockPackage))
                        pendingUnblockPackage.value = null
                    }
                }

                // A printed Shield scanned outside the app. Handled exactly like a tag tap, so the
                // collector above does the navigating.
                val shieldPayload = pendingShieldPayload.value
                LaunchedEffect(shieldPayload) {
                    if (shieldPayload != null) {
                        shieldScanHandler.handleScan(
                            token = ShieldToken(ShieldTokenKind.QR, shieldPayload),
                            mode = ScanMode.SESSION,
                        )
                        pendingShieldPayload.value = null
                    }
                }

                // "Return to your session" on the boundary — go straight to the running session
                // rather than dropping the person on Home to find their way back.
                val openSession = pendingOpenSession.value
                LaunchedEffect(openSession) {
                    if (openSession) {
                        navController.navigate(Route.Protected.path) {
                            popUpTo(Route.Home.path) { inclusive = false }
                            launchSingleTop = true
                        }
                        pendingOpenSession.value = false
                    }
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingUnblockPackage.value = intent.getStringExtra(EXTRA_UNBLOCK_PACKAGE)
        pendingOpenSession.value = intent.getBooleanExtra(EXTRA_OPEN_SESSION, false)
        pendingShieldPayload.value = shieldPayloadFrom(intent)
        intent.removeExtra(EXTRA_UNBLOCK_PACKAGE)
        intent.removeExtra(EXTRA_OPEN_SESSION)
        intent.data = null
    }

    /** A shield link from an outside scanner, ignoring any other URI that reaches us. */
    private fun shieldPayloadFrom(intent: Intent?): String? =
        intent?.data?.toString()?.takeIf { ShieldQr.isShieldPayload(it) }

    override fun onResume() {
        super.onResume()
        if (nfcManager.isNfcAvailable() && nfcManager.isNfcEnabled()) {
            nfcManager.enableForegroundReader(
                activity = this,
                mode = ScanMode.SESSION,
            )
        }
    }

    override fun onPause() {
        nfcManager.disableForegroundReader(this)
        super.onPause()
    }

    companion object {
        const val EXTRA_UNBLOCK_PACKAGE = "com.haoshield.extra.UNBLOCK_PACKAGE"
        const val EXTRA_OPEN_SESSION = "com.haoshield.extra.OPEN_SESSION"
    }
}
