package com.haoshield.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.BlockingMode
import com.haoshield.ui.components.HaoGlyphButton
import com.haoshield.ui.navigation.Route
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onNavigateToProtected: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToProtected -> onNavigateToProtected()
                HomeEvent.NavigateToSetup -> onNavigate(Route.Setup.path)
                HomeEvent.NavigateToGuide -> onNavigate(Route.Guide.path)
                is HomeEvent.NavigateToScanner -> onNavigate(Route.QrScanner.createRoute(event.mode))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = HaoTheme.spacing.screenH),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // The glyph and its label are one target: "Tap to begin" sitting outside the tap zone was
        // the thing people actually aimed at. The shared interactionSource means pressing the
        // label still drives the glyph's press feedback.
        val glyphInteraction = remember { MutableInteractionSource() }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(
                    interactionSource = glyphInteraction,
                    indication = null,
                    onClickLabel = "Begin protected time",
                    onClick = viewModel::onGlyphClick,
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = glyphContentDescription(uiState)
                },
        ) {
            HaoGlyphButton(
                active = uiState.phase == HomePhase.Active,
                interactionSource = glyphInteraction,
            )

            Crossfade(
                targetState = subtitleFor(uiState),
                animationSpec = tween(HaoMotion.STANDARD),
                label = "subtitle",
            ) { subtitle ->
                Text(
                    text = subtitle,
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = HaoTheme.spacing.md),
                )
            }
        }

        if (uiState.phase == HomePhase.AwaitingShield) {
            if (uiState.hasQrToken) {
                TextButton(
                    onClick = viewModel::onScanPrintedShield,
                    modifier = Modifier.padding(top = HaoTheme.spacing.md),
                ) {
                    Text(
                        text = "Scan your printed Shield",
                        style = HaoTheme.type.caption,
                        color = HaoTheme.colors.ink,
                    )
                }
            }
            TextButton(
                onClick = viewModel::onCancelAwaiting,
                modifier = Modifier.padding(top = HaoTheme.spacing.sm),
            ) {
                Text(
                    text = "Cancel",
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkSoft,
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn(animationSpec = tween(HaoMotion.STANDARD)),
            exit = fadeOut(animationSpec = tween(HaoMotion.STANDARD)),
        ) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Keep the physical ritual discoverable for anyone who hasn't made a Shield yet, rather
        // than leaving it buried in Settings.
        if (uiState.phase == HomePhase.Idle && !uiState.hasAnyToken) {
            TextButton(onClick = viewModel::onMakeShield) {
                Text(
                    text = "Make your Hǎo Shield",
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.ink,
                )
            }
        }

        Row(
            modifier = Modifier.padding(
                top = HaoTheme.spacing.sm,
                bottom = HaoTheme.spacing.xl,
            ),
            horizontalArrangement = Arrangement.spacedBy(HaoTheme.spacing.lg),
        ) {
            TextButton(onClick = { onNavigate(Route.Journal.path) }) {
                Text(text = "Journal", style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
            }
            TextButton(onClick = { onNavigate(Route.Settings.path) }) {
                Text(text = "Settings", style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
            }
        }
        Spacer(modifier = Modifier.height(HaoTheme.spacing.xs))
    }
}

private fun subtitleFor(state: HomeUiState): String = when (state.phase) {
    HomePhase.Active -> when (state.elapsedMinutes) {
        0L -> "Protected · just now"
        1L -> "Protected · 1 minute"
        else -> "Protected · ${state.elapsedMinutes} minutes"
    }
    HomePhase.AwaitingShield -> "Hold your Shield to the back of your phone"
    HomePhase.Idle -> when (state.mode) {
        BlockingMode.SOFTWARE -> "Tap to begin"
        BlockingMode.SHIELD ->
            if (state.hasAnyToken) "Tap your Shield to begin" else "Register your Shield to begin"
    }
}

private fun glyphContentDescription(state: HomeUiState): String = when (state.phase) {
    HomePhase.Active -> "Session in progress, ${state.elapsedMinutes} minutes elapsed. Open session."
    HomePhase.AwaitingShield -> "Waiting for your Shield. Hold it to the back of your phone."
    HomePhase.Idle -> "Begin a protected session"
}
