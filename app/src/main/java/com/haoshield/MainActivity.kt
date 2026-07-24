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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.ShieldScanResult
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingUnblockPackage.value = intent?.getStringExtra(EXTRA_UNBLOCK_PACKAGE)
        // Consume it so a later configuration-change recreate doesn't re-open the unblock screen.
        intent?.removeExtra(EXTRA_UNBLOCK_PACKAGE)
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
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                Box(modifier = Modifier.fillMaxSize()) {
                    HaoShieldNavHost(navController = navController)
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                    )
                }

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
                                navController.navigate(Route.Home.path) {
                                    popUpTo(Route.Home.path) { inclusive = true }
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
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingUnblockPackage.value = intent.getStringExtra(EXTRA_UNBLOCK_PACKAGE)
        intent.removeExtra(EXTRA_UNBLOCK_PACKAGE)
    }

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
    }
}
