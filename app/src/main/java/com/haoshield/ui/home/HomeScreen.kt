package com.haoshield.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.R
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
    val context = LocalContext.current

    // Asked at the moment it means something — you're about to begin protected time, and the
    // notification is what will hold it. Declining is fine: the session runs regardless, just
    // without the reminder.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Granted or not, the session is unaffected. */ }

    fun beginWithNotificationConsent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.onGlyphClick()
    }

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

    // The temple runs the full width of the foot of the screen, so the layout has to know how
    // tall that band comes out at this width in order to keep everything else off it.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val templeBand = maxWidth / TEMPLE_BAND_ASPECT

        // The anchor piece: a far temple among pines at the whisper register, laid along the
        // whole foot of the screen and running under the gesture bar — the ground the screen
        // stands on rather than a picture placed on it. Cropped from the bottom so the band is
        // all drawing and none of the empty sky the render carries above the treeline.
        Image(
            painter = painterResource(R.drawable.ill_home_temple),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(templeBand),
            alignment = Alignment.BottomCenter,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(HaoTheme.colors.inkFaint),
        )

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
                        onClick = ::beginWithNotificationConsent,
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
                    bottom = HaoTheme.spacing.md,
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

            // The treeline's room. Held open here rather than overlapped, so the links sit in clear
            // air above the trees however tall the band comes out on a given screen.
            Spacer(modifier = Modifier.height(templeBand))
        }
    }
}

/**
 * Width-to-height of the temple band at the foot of Home. The render is 720×480 with empty sky
 * above the treeline; keeping the lower 340px puts the roof just inside the top of the band, and
 * expressing it as a ratio means the same crop at every screen width.
 */
private const val TEMPLE_BAND_ASPECT = 2.12f

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
