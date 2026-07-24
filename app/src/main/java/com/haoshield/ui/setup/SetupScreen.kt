package com.haoshield.ui.setup

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.theme.HaoTheme

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
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        HaoBackLink(onClick = onNavigateBack, label = "Not now")

        Text(
            text = "Prepare your Shield",
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
        )
        Text(
            text = "Two gentle permissions let the Shield rest your chosen apps while a session " +
                "is open. You stay in control — turn them off anytime.",
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))

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

        Spacer(modifier = Modifier.height(HaoTheme.spacing.lg))

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

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))

        HaoPrimaryButton(
            text = when {
                uiState.isStartingSession -> "Starting…"
                uiState.isReady -> "Begin Software session"
                else -> "Grant both to begin"
            },
            onClick = viewModel::onStartSoftwareSession,
            enabled = uiState.isReady && !uiState.isStartingSession,
            modifier = Modifier.fillMaxWidth(),
        )
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
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (granted) "✓" else index.toString(),
                style = HaoTheme.type.heading,
                color = HaoTheme.colors.ink,
            )
        }

        Spacer(modifier = Modifier.width(HaoTheme.spacing.md))

        Column {
            Text(
                text = title,
                style = HaoTheme.type.heading,
                color = HaoTheme.colors.ink,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
            )
            if (granted) {
                Text(
                    text = "Enabled",
                    modifier = Modifier.padding(top = HaoTheme.spacing.sm),
                    style = HaoTheme.type.label,
                    color = HaoTheme.colors.ink,
                )
            } else {
                TextButton(onClick = onOpenSettings) {
                    Text(
                        text = "Open settings",
                        style = HaoTheme.type.label,
                        color = HaoTheme.colors.ink,
                    )
                }
            }
        }
    }
}
