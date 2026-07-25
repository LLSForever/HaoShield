package com.haoshield.ui.protectedscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.SessionMode
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoSecondaryButton
import com.haoshield.ui.components.HaoTextField
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme

@Composable
fun ProtectedScreen(
    onNavigateHome: () -> Unit,
    onNavigateToReflection: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: ProtectedScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProtectedScreenEvent.NavigateHome -> onNavigateHome()
                ProtectedScreenEvent.NavigateToReflection -> onNavigateToReflection()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AmbientMusicToggle(
            isPlaying = uiState.isAmbientMusicPlaying,
            onToggle = viewModel::onToggleAmbientMusic,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = HaoTheme.spacing.md, end = HaoTheme.spacing.sm),
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
                style = HaoTheme.type.clock,
                color = HaoTheme.colors.ink,
                // Never let a long duration wrap onto a second line.
                maxLines = 1,
                softWrap = false,
            )

            Text(
                text = uiState.protectionMessage,
                modifier = Modifier.padding(top = HaoTheme.spacing.lg),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
                textAlign = TextAlign.Center,
            )

            // A gentle, ignorable invitation to name what this time is for. Fades away once set,
            // dismissed, or after the early window passes.
            AnimatedVisibility(
                visible = uiState.showIntentionPrompt,
                enter = fadeIn(animationSpec = tween(durationMillis = HaoMotion.GENTLE)),
                exit = fadeOut(animationSpec = tween(durationMillis = HaoMotion.STANDARD)),
                modifier = Modifier.padding(top = HaoTheme.spacing.xl),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "What is this time for?",
                        style = HaoTheme.type.caption,
                        color = HaoTheme.colors.inkFaint,
                    )
                    HaoTextField(
                        value = uiState.intentionDraft,
                        onValueChange = viewModel::onIntentionDraftChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = HaoTheme.spacing.sm),
                        placeholder = "A few words, if you like",
                        minLines = 1,
                    )
                    Row(
                        modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(HaoTheme.spacing.md),
                    ) {
                        TextButton(onClick = viewModel::onDismissIntentionPrompt) {
                            Text(
                                text = "Not now",
                                style = HaoTheme.type.caption,
                                color = HaoTheme.colors.inkFaint,
                            )
                        }
                        TextButton(onClick = viewModel::onSubmitIntention) {
                            Text(
                                text = "Set intention",
                                style = HaoTheme.type.caption,
                                color = HaoTheme.colors.ink,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.quoteVisible && uiState.currentQuote != null,
                enter = fadeIn(animationSpec = tween(durationMillis = HaoMotion.GENTLE)),
                exit = fadeOut(animationSpec = tween(durationMillis = HaoMotion.GENTLE)),
                modifier = Modifier.padding(top = HaoTheme.spacing.xxl),
            ) {
                Text(
                    text = uiState.currentQuote.orEmpty(),
                    style = HaoTheme.type.voice.copy(fontStyle = FontStyle.Italic),
                    color = HaoTheme.colors.inkSoft,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = HaoTheme.spacing.xl,
                    start = HaoTheme.spacing.xl,
                    end = HaoTheme.spacing.xl,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.endSessionHint?.let { hint ->
                Text(
                    text = hint,
                    modifier = Modifier.padding(bottom = HaoTheme.spacing.sm),
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkSoft,
                    textAlign = TextAlign.Center,
                )
            }

            HaoSecondaryButton(
                text = if (uiState.isEndingSession) "Ending…" else uiState.endButtonLabel,
                onClick = viewModel::onEndSessionClick,
                enabled = !uiState.isEndingSession,
            )

            if (uiState.sessionMode == SessionMode.SHIELD && uiState.endSessionHint == null) {
                Text(
                    text = "Tap your Hǎo Shield to end this session.",
                    modifier = Modifier.padding(top = HaoTheme.spacing.sm),
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkFaint,
                    textAlign = TextAlign.Center,
                )
            }

            if (uiState.sessionMode == SessionMode.SHIELD && uiState.hasQrToken) {
                TextButton(
                    onClick = onNavigateToScanner,
                    modifier = Modifier.padding(top = HaoTheme.spacing.sm),
                ) {
                    Text(
                        text = "Scan your printed Shield to end",
                        style = HaoTheme.type.caption,
                        color = HaoTheme.colors.ink,
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
                        color = HaoTheme.colors.inkFaint,
                    )
                }
            }
        }

        // Fade the emergency panel in and out; remember the last step so the exit fade has content.
        var lastEmergencyStep by remember { mutableStateOf(EmergencyExitStep.WRITING_NOTE) }
        uiState.emergencyStep?.let { lastEmergencyStep = it }
        AnimatedVisibility(
            visible = uiState.emergencyStep != null,
            enter = fadeIn(animationSpec = tween(HaoMotion.STANDARD)),
            exit = fadeOut(animationSpec = tween(HaoMotion.STANDARD)),
            modifier = Modifier.matchParentSize(),
        ) {
            EmergencyExitPanel(
                step = uiState.emergencyStep ?: lastEmergencyStep,
                note = uiState.emergencyNote,
                countdownSeconds = uiState.emergencyCountdownSeconds,
                onNoteChange = viewModel::onEmergencyNoteChange,
                onStartCountdown = viewModel::onStartEmergencyCountdown,
                onCancel = viewModel::onCancelEmergencyExit,
                modifier = Modifier.fillMaxSize(),
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
                HaoTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HaoTheme.spacing.lg),
                    placeholder = "A short, honest intention…",
                    minLines = 3,
                )
                HaoPrimaryButton(
                    text = "Continue",
                    onClick = onStartCountdown,
                    enabled = note.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HaoTheme.spacing.md),
                )
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = HaoTheme.spacing.xs),
                ) {
                    Text(
                        text = "Stay in the session",
                        style = HaoTheme.type.body,
                        color = HaoTheme.colors.inkSoft,
                    )
                }
            }

            EmergencyExitStep.COUNTDOWN -> {
                // The one seal moment of this screen: an irreversible commitment underway.
                Text(
                    text = countdownSeconds.toString(),
                    style = HaoTheme.type.timer,
                    color = HaoTheme.colors.seal,
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
            style = if (isPlaying) HaoTheme.type.caption else HaoTheme.type.voice,
            color = if (isPlaying) HaoTheme.colors.ink else HaoTheme.colors.inkFaint,
        )
    }
}
