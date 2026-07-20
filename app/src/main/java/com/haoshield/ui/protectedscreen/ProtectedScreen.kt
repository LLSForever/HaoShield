package com.haoshield.ui.protectedscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.SessionMode
import com.haoshield.ui.theme.MutedText
import com.haoshield.ui.theme.Sage
import com.haoshield.ui.theme.WarmBackground

@Composable
fun ProtectedScreen(
    onNavigateHome: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: ProtectedScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProtectedScreenEvent.NavigateHome -> onNavigateHome()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AmbientMusicToggle(
            isPlaying = uiState.isAmbientMusicPlaying,
            onToggle = viewModel::onToggleAmbientMusic,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = uiState.formattedElapsedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                ),
                color = Sage,
            )

            Text(
                text = uiState.protectionMessage,
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedText,
                textAlign = TextAlign.Center,
            )

            AnimatedVisibility(
                visible = uiState.quoteVisible && uiState.currentQuote != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 2_000)),
                exit = fadeOut(animationSpec = tween(durationMillis = 2_000)),
                modifier = Modifier.padding(top = 48.dp),
            ) {
                Text(
                    text = uiState.currentQuote.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = MutedText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.endSessionHint?.let { hint ->
                Text(
                    text = hint,
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                )
            }

            TextButton(
                onClick = viewModel::onEndSessionClick,
                enabled = !uiState.isEndingSession,
            ) {
                Text(
                    text = if (uiState.isEndingSession) "Ending…" else uiState.endButtonLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Sage,
                )
            }

            if (uiState.sessionMode == SessionMode.SHIELD && uiState.endSessionHint == null) {
                Text(
                    text = "Tap your Hǎo Shield to end this session.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            if (uiState.sessionMode == SessionMode.SHIELD && uiState.hasQrToken) {
                TextButton(
                    onClick = onNavigateToScanner,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = "Scan your printed Shield to end",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage.copy(alpha = 0.8f),
                    )
                }
            }

            if (uiState.sessionMode == SessionMode.SHIELD) {
                TextButton(
                    onClick = viewModel::onRequestEmergencyExit,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = "I don't have my Shield with me",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText.copy(alpha = 0.6f),
                    )
                }
            }
        }

        uiState.emergencyStep?.let { emergencyStep ->
            EmergencyExitPanel(
                step = emergencyStep,
                note = uiState.emergencyNote,
                countdownSeconds = uiState.emergencyCountdownSeconds,
                onNoteChange = viewModel::onEmergencyNoteChange,
                onStartCountdown = viewModel::onStartEmergencyCountdown,
                onCancel = viewModel::onCancelEmergencyExit,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun EmergencyExitPanel(
    step: EmergencyExitStep,
    note: String,
    countdownSeconds: Int,
    onNoteChange: (String) -> Unit,
    onStartCountdown: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            EmergencyExitStep.WRITING_NOTE -> {
                Text(
                    text = "Ending without your Shield",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
                    color = Sage,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Write what brought you here. Your session will end after a " +
                        "minute of stillness.",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    placeholder = {
                        Text(
                            text = "A short, honest intention…",
                            color = MutedText.copy(alpha = 0.5f),
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sage.copy(alpha = 0.6f),
                        unfocusedBorderColor = MutedText.copy(alpha = 0.25f),
                        cursorColor = Sage,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    minLines = 3,
                )
                TextButton(
                    onClick = onStartCountdown,
                    enabled = note.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (note.isNotBlank()) Sage else MutedText,
                    )
                }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Stay in the session",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedText,
                    )
                }
            }

            EmergencyExitStep.COUNTDOWN -> {
                Text(
                    text = countdownSeconds.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                    ),
                    color = Sage,
                )
                Text(
                    text = "Breathe. The session will end on its own.",
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(top = 32.dp),
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedText,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientMusicToggle(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onToggle,
        modifier = modifier,
    ) {
        Text(
            text = if (isPlaying) "❚❚" else "♪",
            fontSize = if (isPlaying) 14.sp else 22.sp,
            color = if (isPlaying) Sage else MutedText.copy(alpha = 0.38f),
        )
    }
}