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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.SessionMode
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoSecondaryButton
import com.haoshield.ui.components.HaoTextField
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme
import kotlinx.coroutines.delay

@Composable
fun ProtectedScreen(
    openEmergencyExit: Boolean = false,
    onNavigateHome: () -> Unit,
    onNavigateToReflection: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: ProtectedScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Arrived through the door at the foot of Settings.
    LaunchedEffect(openEmergencyExit) {
        if (openEmergencyExit) viewModel.onRequestEmergencyExit()
    }

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
    // True while the intention is being written, so the rest of the screen can withdraw.
    var writingIntention by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

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
            ) {
                // While writing, a tap away puts the words down rather than darkening the screen.
                if (writingIntention) focusManager.clearFocus() else resting = true
            },
    ) {
        AmbientMusicToggle(
            isPlaying = uiState.isAmbientMusicPlaying,
            onToggle = viewModel::onToggleAmbientMusic,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = HaoTheme.spacing.md, end = HaoTheme.spacing.sm),
        )

        // Naming an intention is its own moment. While the words are being written the rest of
        // the screen withdraws — no clock counting at you, no ways out in view. Tapping away
        // returns it.
        val stillness by animateFloatAsState(
            targetValue = if (writingIntention) 0f else 1f,
            animationSpec = tween(HaoMotion.STANDARD),
            label = "stillness",
        )

        // Shown once, early, then gone. The gesture only needs teaching the first time.
        var showRestHint by rememberSaveable { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(REST_HINT_MILLIS)
            showRestHint = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // Reserve the keyboard's height, whatever a given keyboard's is, so the field it
                // raises is never sat on. Falls back to the navigation bar when no keyboard is up.
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = HaoTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Weighted so the still centre sits a little above the middle, leaving the lower
            // half room to breathe. While writing, the top weight eases down so the field rises
            // into the upper half — with the keyboard up, that is the only space to rise into.
            Spacer(modifier = Modifier.weight(lerp(0.5f, 0.8f, stillness)))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Withdraw from the layout as well as the eye: as this fades it also gives up its
                // height, so the field below climbs into the freed space instead of the field
                // staying pinned low with a tall empty gap where the clock used to be.
                modifier = Modifier.withdraw(stillness),
            ) {
                Text(
                    text = "好",
                    modifier = Modifier.padding(bottom = HaoTheme.spacing.lg),
                    style = HaoTheme.type.glyphMark,
                    color = HaoTheme.colors.inkFaint,
                )

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

                AnimatedVisibility(
                    visible = showRestHint,
                    exit = fadeOut(animationSpec = tween(durationMillis = HaoMotion.GENTLE)),
                ) {
                    Text(
                        text = "Tap anywhere to rest the screen.",
                        modifier = Modifier.padding(top = HaoTheme.spacing.sm),
                        style = HaoTheme.type.caption,
                        color = HaoTheme.colors.inkFaint,
                        textAlign = TextAlign.Center,
                    )
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

            Spacer(modifier = Modifier.weight(lerp(1.5f, 1.2f, stillness)))

            // The invitation to name the time — the one thing that stays present while writing.
            AnimatedVisibility(
                visible = uiState.showIntentionPrompt,
                enter = fadeIn(animationSpec = tween(durationMillis = HaoMotion.GENTLE)),
                exit = fadeOut(animationSpec = tween(durationMillis = HaoMotion.STANDARD)),
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
                            .padding(top = HaoTheme.spacing.sm)
                            .onFocusChanged { writingIntention = it.isFocused },
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

            // How this ends: one affordance, and one quiet way past it. A Shield session ends at
            // the Shield — saying so is enough. A button whose only job was to tell you to go and
            // use the object was the app explaining itself.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = HaoTheme.spacing.xl)
                    .withdraw(stillness),
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

                when {
                    uiState.sessionMode != SessionMode.SHIELD -> HaoSecondaryButton(
                        text = if (uiState.isEndingSession) "Ending…" else "End session",
                        onClick = viewModel::onEndSessionClick,
                        enabled = !uiState.isEndingSession,
                    )
                    uiState.hasNfcToken -> Text(
                        text = "Tap your Shield to end.",
                        style = HaoTheme.type.body,
                        color = HaoTheme.colors.inkSoft,
                        textAlign = TextAlign.Center,
                    )
                    uiState.hasQrToken -> HaoSecondaryButton(
                        text = "Scan your Shield",
                        onClick = onNavigateToScanner,
                    )
                    else -> Unit
                }

            }

            Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))
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
        initialValue = 0.20f,
        targetValue = 0.38f,
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

/**
 * Fade a piece of the screen out *and* let it surrender its height at the same rate, so siblings
 * below rise into the space rather than leaving a hole where the faded content still stands. At
 * [fraction] 1 it is an identity (full height, full opacity); at 0 it occupies nothing.
 */
private fun Modifier.withdraw(fraction: Float): Modifier = this
    .graphicsLayer { alpha = fraction }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = (placeable.height * fraction).roundToInt()
        layout(placeable.width, height) { placeable.place(0, 0) }
    }

private const val RESTING_BRIGHTNESS = 0.01f

/** How long the rest-the-screen gesture is explained before the screen falls quiet. */
private const val REST_HINT_MILLIS = 20_000L

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
            // The note field raises the keyboard; keep the panel's content above it.
            .imePadding()
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
