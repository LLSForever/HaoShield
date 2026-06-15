package com.haoshield

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import com.haoshield.domain.model.NfcReaderMode
import com.haoshield.domain.service.NfcManager
import com.haoshield.ui.navigation.HaoShieldNavHost
import com.haoshield.ui.navigation.Route
import com.haoshield.ui.theme.HaoShieldTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var nfcManager: NfcManager

    // Set when the blocking overlay asks us to open the unblock screen for a specific app.
    private val pendingUnblockPackage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingUnblockPackage.value = intent?.getStringExtra(EXTRA_UNBLOCK_PACKAGE)
        setContent {
            HaoShieldTheme {
                val navController = rememberNavController()
                HaoShieldNavHost(navController = navController)

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
    }

    override fun onResume() {
        super.onResume()
        if (nfcManager.isNfcAvailable() && nfcManager.isNfcEnabled()) {
            nfcManager.enableForegroundReader(
                activity = this,
                mode = NfcReaderMode.SESSION,
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
