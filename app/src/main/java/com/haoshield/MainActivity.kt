package com.haoshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.haoshield.domain.model.NfcReaderMode
import com.haoshield.domain.service.NfcManager
import com.haoshield.ui.navigation.HaoShieldNavHost
import com.haoshield.ui.theme.HaoShieldTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var nfcManager: NfcManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HaoShieldTheme {
                val navController = rememberNavController()
                HaoShieldNavHost(navController = navController)
            }
        }
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
}