package com.haoshield.ui.setup

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val WarmBackground = Color(0xFFF7F5F0)
private val Sage = Color(0xFF5C6B5C)
private val MutedText = Color(0xFF6B6B68)

@Composable
fun SetupScreen(
    onNavigateToProtected: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-check on every resume so returning from system settings reflects the new state.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SetupEvent.NavigateToProtected -> onNavigateToProtected()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
    ) {
        Text(
            text = "Prepare your Shield",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
            color = Sage,
        )
        Text(
            text = "Two gentle permissions let the Shield rest your chosen apps while a session " +
                "is open. You stay in control — turn them off anytime.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MutedText,
        )

        Spacer(modifier = Modifier.height(32.dp))

        SetupStep(
            index = 1,
            title = "Let the Shield notice the open app",
            description = "Enable Hǎo Shield in Accessibility settings so it can tell when a " +
                "blocked app comes to the front.",
            granted = uiState.isAccessibilityEnabled,
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        SetupStep(
            index = 2,
            title = "Let the calm screen appear",
            description = "Allow Hǎo Shield to display over other apps, so the protected screen " +
                "can gently take their place.",
            granted = uiState.canDrawOverlay,
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
        )

        Spacer(modifier = Modifier.height(40.dp))

        TextButton(
            onClick = viewModel::onStartSoftwareSession,
            enabled = uiState.isReady && !uiState.isStartingSession,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = when {
                    uiState.isStartingSession -> "Starting…"
                    uiState.isReady -> "Begin Software session"
                    else -> "Grant both to begin"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (uiState.isReady) Sage else MutedText,
            )
        }

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Not now",
                style = MaterialTheme.typography.labelLarge,
                color = MutedText.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun SetupStep(
    index: Int,
    title: String,
    description: String,
    granted: Boolean,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (granted) "✓" else index.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Sage,
            )
            Text(
                text = title,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Sage,
            )
        }
        Text(
            text = description,
            modifier = Modifier.padding(top = 6.dp, start = 28.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
        )
        if (granted) {
            Text(
                text = "Enabled",
                modifier = Modifier.padding(top = 8.dp, start = 28.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Sage,
            )
        } else {
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
            ) {
                Text(
                    text = "Open settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = Sage,
                )
            }
        }
    }
}
