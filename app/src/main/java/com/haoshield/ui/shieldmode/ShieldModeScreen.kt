package com.haoshield.ui.shieldmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.SessionMode
import com.haoshield.ui.theme.MutedText
import com.haoshield.ui.theme.Sage
import com.haoshield.ui.theme.WarmBackground

@Composable
fun ShieldModeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToProtected: () -> Unit,
    viewModel: ShieldModeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        TextButton(onClick = onNavigateBack) {
            Text(text = "Back", color = MutedText)
        }

        Text(
            text = "Shield Mode",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
            color = Sage,
        )

        Spacer(modifier = Modifier.height(24.dp))

        when {
            !uiState.hasAnyToken -> NoTokenState(onNavigateToGuide = onNavigateToGuide)
            uiState.hasActiveSession -> ActiveSessionState(
                sessionMode = uiState.activeSessionMode,
                onNavigateToProtected = onNavigateToProtected,
            )
            !uiState.permissionsReady -> PermissionsNeededState(onNavigateToSetup = onNavigateToSetup)
            else -> ReadyState(
                uiState = uiState,
                onNavigateToScanner = onNavigateToScanner,
                onNavigateToGuide = onNavigateToGuide,
            )
        }
    }
}

@Composable
private fun NoTokenState(onNavigateToGuide: () -> Unit) {
    Text(
        text = "Shield Mode begins with a physical Hǎo Shield — an NFC tag you tap, or a " +
            "printed code you scan. Make one, and this becomes your quiet ritual.",
        style = MaterialTheme.typography.bodyLarge,
        color = MutedText,
    )
    Spacer(modifier = Modifier.height(32.dp))
    TextButton(
        onClick = onNavigateToGuide,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Make & register a Shield",
            style = MaterialTheme.typography.titleMedium,
            color = Sage,
        )
    }
}

@Composable
private fun PermissionsNeededState(onNavigateToSetup: () -> Unit) {
    Text(
        text = "Shield Mode needs the same two permissions as Software Mode, so it can rest " +
            "your chosen apps. Grant them once and you're set.",
        style = MaterialTheme.typography.bodyLarge,
        color = MutedText,
    )
    Spacer(modifier = Modifier.height(32.dp))
    TextButton(
        onClick = onNavigateToSetup,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Set up protection",
            style = MaterialTheme.typography.titleMedium,
            color = Sage,
        )
    }
}

@Composable
private fun ReadyState(
    uiState: ShieldModeUiState,
    onNavigateToScanner: () -> Unit,
    onNavigateToGuide: () -> Unit,
) {
    val registeredSummary = buildList {
        if (uiState.hasNfcToken) add("an NFC tag")
        if (uiState.hasQrToken) add("a printed code")
    }.joinToString(" and ")

    Text(
        text = "You have $registeredSummary registered.",
        style = MaterialTheme.typography.bodyMedium,
        color = MutedText,
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (uiState.hasNfcToken) {
        Text(
            text = if (uiState.isNfcHardwareAvailable) {
                "Tap your Shield against the back of your phone to begin protected time."
            } else {
                "This device has no NFC reader — use your printed code to begin."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    } else {
        Text(
            text = "Scan your printed Shield to begin protected time.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }

    if (uiState.hasQrToken) {
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = onNavigateToScanner,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Scan your printed Shield",
                style = MaterialTheme.typography.titleMedium,
                color = Sage,
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    TextButton(
        onClick = onNavigateToGuide,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Register another Shield",
            style = MaterialTheme.typography.labelLarge,
            color = MutedText,
        )
    }
}

@Composable
private fun ActiveSessionState(
    sessionMode: SessionMode?,
    onNavigateToProtected: () -> Unit,
) {
    Text(
        text = "A session is protecting you right now.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )

    if (sessionMode == SessionMode.SOFTWARE) {
        Text(
            text = "It's a Software session — tapping your Shield won't affect it. End it from " +
                "the session screen.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
    TextButton(
        onClick = onNavigateToProtected,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Go to your session",
            style = MaterialTheme.typography.titleMedium,
            color = Sage,
        )
    }
}
