package com.haoshield.ui.protectedscreen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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

    // Tapping empty space rests the screen — the session keeps running in the dark. Ephemeral view
    // state on purpose: nothing outside this screen needs to know, it just has to survive rotation.
    var resting by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Actually dim the backlight while resting, so it's darkness rather than just black pixels.
    DisposableEffect(resting) {
        if (resting) context.setScreenBrightness(RESTING_BRIGHTNESS)
        onDispose {
            // Also on dispose — leaving the screen while resting must never strand a dark display.
            context.setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }

    // Insets sit on the children rather than the root, so the resting surface can cover the status
    // and navigation bars too. (EmergencyExitPanel already applies its own.)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                // Not during the emergency countdown — you shouldn't be able to black that out.
                enabled = uiState.emergencyStep == null && !resting,
                onClickLabel = "Rest the screen",
            ) { resting = true },
    ) {
        AmbientMusicToggle(
            isPlaying = uiState.isAmbientMusicPlaying,
            onToggle = viewModel::onToggleAmbientMusic,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = HaoTheme.spacing.md, end = HaoTheme.spacing.sm),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
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

            Text(
                text = "Tap anywhere to rest the screen.",
                modifier = Modifier.padding(top = HaoTheme.spacing.sm),
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.inkFaint,
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
                .navigationBarsPadding()
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

        // Last child, and outside every inset — the dark covers the system bars too.
        AnimatedVisibility(
            visible = resting,
            enter = fadeIn(animationSpec = tween(HaoMotion.SLOW)),
            exit = fadeOut(animationSpec = tween(HaoMotion.SLOW)),
            modifier = Modifier.matchParentSize(),
        ) {
            RestingSurface(onWake = { resting = false })
        }
    }
}

/**
 * The screen at rest: black, with a barely-there 好 breathing so it reads as "still protected"
 * rather than "off" or "crashed". Swallows every tap so none reaches the controls beneath.
 */
@Composable
private fun RestingSurface(onWake: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "resting")
    val glyphAlpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(HaoMotion.BREATH, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glyphAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Deliberately true black, not the Dusk ground: this is a screen at rest, and black is
            // what actually turns OLED pixels off. A palette sweep should leave this alone.
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Show the session",
                onClick = onWake,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "好",
            style = HaoTheme.type.glyphSmall,
            color = HaoTheme.colors.inkSoft,
            modifier = Modifier.graphicsLayer { alpha = glyphAlpha },
        )
    }
}

private const val RESTING_BRIGHTNESS = 0.01f

private fun Context.setScreenBrightness(brightness: Float) {
    val activity = generateSequence(this) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>()
        .firstOrNull()
        ?: return
    activity.window?.let { window ->
        window.attributes = window.attributes.apply { screenBrightness = brightness }
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
