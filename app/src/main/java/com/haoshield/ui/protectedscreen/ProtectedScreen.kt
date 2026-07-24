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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.SessionMode
import com.haoshield.ui.theme.HaoTheme

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
            .background(HaoTheme.colors.paper)
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
                .padding(horizontal = HaoTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = uiState.formattedElapsedTime,
                style = HaoTheme.type.display.copy(fontSize = 72.sp, letterSpacing = 2.sp),
                color = HaoTheme.colors.ink,
            )

            Text(
                text = uiState.protectionMessage,
                modifier = Modifier.padding(top = HaoTheme.spacing.lg),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
                textAlign = TextAlign.Center,
            )

            AnimatedVisibility(
                visible = uiState.quoteVisible && uiState.currentQuote != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 2_000)),
                exit = fadeOut(animationSpec = tween(durationMillis = 2_000)),
                modifier = Modifier.padding(top = HaoTheme.spacing.xxl),
            ) {
                Text(
                    text = uiState.currentQuote.orEmpty(),
                    style = HaoTheme.type.voice.copy(fontStyle = FontStyle.Italic),
                    color = HaoTheme.colors.inkSoft.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = HaoTheme.spacing.xl, start = HaoTheme.spacing.xl, end = HaoTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.endSessionHint?.let { hint ->
                Text(
                    text = hint,
                    modifier = Modifier.padding(bottom = HaoTheme.spacing.sm),
                    style = HaoTheme.type.body,
                    color = HaoTheme.colors.inkSoft,
                    textAlign = TextAlign.Center,
                )
            }

            TextButton(
                onClick = viewModel::onEndSessionClick,
                enabled = !uiState.isEndingSession,
            ) {
                Text(
                    text = if (uiState.isEndingSession) "Ending…" else uiState.endButtonLabel,
                    style = HaoTheme.type.body,
                    color = HaoTheme.colors.ink,
                )
            }

            if (uiState.sessionMode == SessionMode.SHIELD && uiState.endSessionHint == null) {
                Text(
                    text = "Tap your Hǎo Shield to end this session.",
                    modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkSoft.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            if (uiState.sessionMode == SessionMode.SHIELD && uiState.hasQrToken) {
                TextButton(
                    onClick = onNavigateToScanner,
                    modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                ) {
                    Text(
                        text = "Scan your printed Shield to end",
                        style = HaoTheme.type.caption,
                        color = HaoTheme.colors.ink.copy(alpha = 0.8f),
                    )
                }
            }

            if (uiState.sessionMode == SessionMode.SHIELD) {
                TextButton(
                    onClick = viewModel::onRequestEmergencyExit,
                    modifier = Modifier.padding(top = HaoTheme.spacing.sm),
                ) {
                    Text(
                        text = "I don't have my Shield with me",
                        style = HaoTheme.type.caption,
                        color = HaoTheme.colors.inkSoft.copy(alpha = 0.6f),
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
            .background(HaoTheme.colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = HaoTheme.spacing.xl, vertical = HaoTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (step) {
            EmergencyExitStep.WRITING_NOTE -> {
                Text(
                    text = "Ending without your Shield",
                    style = HaoTheme.type.heading,
                    color = HaoTheme.colors.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Write what brought you here. Your session will end after a " +
                        "minute of stillness.",
                    modifier = Modifier.padding(top = HaoTheme.spacing.md),
                    style = HaoTheme.type.body,
                    color = HaoTheme.colors.inkSoft,
                    textAlign = TextAlign.Center,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HaoTheme.spacing.lg),
                    placeholder = {
                        Text(
                            text = "A short, honest intention…",
                            style = HaoTheme.type.body,
                            color = HaoTheme.colors.inkSoft.copy(alpha = 0.5f),
                        )
                    },
                    textStyle = HaoTheme.type.body,
                    shape = HaoTheme.shapes.card,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HaoTheme.colors.ink.copy(alpha = 0.6f),
                        unfocusedBorderColor = HaoTheme.colors.inkSoft.copy(alpha = 0.25f),
                        cursorColor = HaoTheme.colors.ink,
                        focusedTextColor = HaoTheme.colors.ink,
                        unfocusedTextColor = HaoTheme.colors.ink,
                    ),
                    minLines = 3,
                )
                TextButton(
                    onClick = onStartCountdown,
                    enabled = note.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HaoTheme.spacing.md),
                ) {
                    Text(
                        text = "Continue",
                        style = HaoTheme.type.body,
                        color = if (note.isNotBlank()) HaoTheme.colors.ink else HaoTheme.colors.inkSoft,
                    )
                }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Stay in the session",
                        style = HaoTheme.type.body,
                        color = HaoTheme.colors.inkSoft,
                    )
                }
            }

            EmergencyExitStep.COUNTDOWN -> {
                Text(
                    text = countdownSeconds.toString(),
                    style = HaoTheme.type.display.copy(fontSize = 72.sp),
                    color = HaoTheme.colors.ink,
                )
                Text(
                    text = "Breathe. The session will end on its own.",
                    modifier = Modifier.padding(top = HaoTheme.spacing.lg),
                    style = HaoTheme.type.voice,
                    color = HaoTheme.colors.inkSoft,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(top = HaoTheme.spacing.xl),
                ) {
                    Text(
                        text = "Cancel",
                        style = HaoTheme.type.body,
                        color = HaoTheme.colors.inkSoft,
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
            color = if (isPlaying) HaoTheme.colors.ink else HaoTheme.colors.inkSoft.copy(alpha = 0.38f),
        )
    }
}
