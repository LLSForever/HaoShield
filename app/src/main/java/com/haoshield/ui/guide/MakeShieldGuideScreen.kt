package com.haoshield.ui.guide

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val WarmBackground = Color(0xFFF7F5F0)
private val Sage = Color(0xFF5C6B5C)
private val MutedText = Color(0xFF6B6B68)
private val SoftAccent = Color(0xFFE8E4DC)

@Composable
fun MakeShieldGuideScreen(
    onNavigateBack: () -> Unit,
    viewModel: MakeShieldGuideViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as ComponentActivity

    when (uiState.step) {
        GuideStep.CONTENT -> GuideContentStep(
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onBeginRegistration = viewModel::onBeginRegistration,
        )
        GuideStep.REGISTER -> {
            DisposableEffect(Unit) {
                viewModel.onEnterRegistrationMode(activity)
                onDispose { viewModel.onLeaveRegistrationMode(activity) }
            }
            RegisterShieldStep(
                uiState = uiState,
                onBack = viewModel::onBackToContent,
            )
        }
        GuideStep.SUCCESS -> RegistrationSuccessStep(
            onContinue = viewModel::onDismissSuccess,
        )
    }
}

@Composable
private fun GuideContentStep(
    uiState: MakeShieldGuideUiState,
    onNavigateBack: () -> Unit,
    onBeginRegistration: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        TextButton(onClick = onNavigateBack) {
            Text(text = "Back", color = MutedText)
        }

        Text(
            text = MakeShieldGuideContent.screenTitle,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
            color = Sage,
        )

        Text(
            text = MakeShieldGuideContent.introduction,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = MakeShieldGuideContent.physicalStrengthMessage,
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .background(SoftAccent)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Sage,
        )

        MakeShieldGuideContent.sections.forEach { section ->
            Text(
                text = section.title,
                modifier = Modifier.padding(top = 28.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Sage,
            )
            Text(
                text = section.body,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
            )
        }

        if (uiState.registeredUid != null) {
            Text(
                text = MakeShieldGuideContent.alreadyRegistered,
                modifier = Modifier.padding(top = 28.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MutedText.copy(alpha = 0.85f),
            )
        }

        Text(
            text = MakeShieldGuideContent.registerPrompt,
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onBeginRegistration,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (uiState.registeredUid == null) {
                    "Register your Shield"
                } else {
                    "Register a new Shield"
                },
                style = MaterialTheme.typography.labelLarge,
                color = Sage,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterShieldStep(
    uiState: MakeShieldGuideUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(text = "Back", color = MutedText)
        }

        Text(
            text = "Register your Shield",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
            color = Sage,
        )

        when {
            !uiState.isNfcAvailable -> {
                Text(
                    text = "This device does not support NFC. You can still use Software Mode.",
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                )
            }
            !uiState.isNfcEnabled -> {
                Text(
                    text = "Please enable NFC in your device settings, then return here.",
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                )
            }
            else -> {
                Text(
                    text = MakeShieldGuideContent.registerInstruction,
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text = "Waiting for your Shield…",
                    modifier = Modifier.padding(top = 32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText.copy(alpha = 0.8f),
                )
            }
        }

        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun RegistrationSuccessStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = MakeShieldGuideContent.registerSuccess,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
            color = Sage,
        )

        Text(
            text = "Tap your Shield to begin and end protected time in Shield Mode.",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.labelLarge,
                color = Sage,
            )
        }
    }
}