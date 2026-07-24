package com.haoshield.ui.guide

import androidx.activity.ComponentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.ScanMode
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoSecondaryButton
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme

@Composable
fun MakeShieldGuideScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: (ScanMode) -> Unit,
    viewModel: MakeShieldGuideViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // NFC registration mode is tied to being on the REGISTER step. Hoisted here (not inside the
    // step content) because under Crossfade both old and new step contents compose during the
    // transition — an effect inside the branch would race enable/disable.
    val isRegisterStep = uiState.step == GuideStep.REGISTER
    DisposableEffect(isRegisterStep) {
        if (isRegisterStep) viewModel.onEnterRegistrationMode(activity)
        onDispose {
            if (isRegisterStep) viewModel.onLeaveRegistrationMode(activity)
        }
    }

    Crossfade(
        targetState = uiState.step,
        animationSpec = tween(HaoMotion.STANDARD),
        label = "guideStep",
    ) { step ->
        when (step) {
            GuideStep.CONTENT -> GuideContentStep(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onBeginRegistration = viewModel::onBeginRegistration,
            )
            GuideStep.CHOOSE_METHOD -> ChooseMethodStep(
                isGeneratingQr = uiState.isGeneratingQr,
                onChooseNfc = viewModel::onChooseNfcMethod,
                onChooseQr = viewModel::onChooseQrMethod,
                onBack = viewModel::onBackToContent,
            )
            GuideStep.REGISTER -> RegisterShieldStep(
                uiState = uiState,
                onBack = viewModel::onBackToChooseMethod,
            )
            GuideStep.QR_DISPLAY -> QrDisplayStep(
                uiState = uiState,
                onPrint = { uiState.qrBitmap?.let { ShieldQrPrinter.print(context, it) } },
                onSaveImage = { uiState.qrBitmap?.let { ShieldQrSharing.share(context, it) } },
                onConfirmScan = { onNavigateToScanner(ScanMode.REGISTRATION) },
                onBack = viewModel::onBackToChooseMethod,
            )
            GuideStep.SUCCESS -> RegistrationSuccessStep(
                message = uiState.statusMessage ?: MakeShieldGuideContent.registerSuccess,
                onContinue = viewModel::onDismissSuccess,
            )
        }
    }
}

@Composable
private fun ScreenColumn(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HaoTheme.spacing.screenH),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
private fun GuideContentStep(
    uiState: MakeShieldGuideUiState,
    onNavigateBack: () -> Unit,
    onBeginRegistration: () -> Unit,
) {
    ScreenColumn {
        HaoBackLink(onClick = onNavigateBack)

        // Hero.
        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "好",
                style = HaoTheme.type.glyphSmall,
                color = HaoTheme.colors.ink,
            )
            Text(
                text = MakeShieldGuideContent.screenTitle,
                style = HaoTheme.type.display,
                color = HaoTheme.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
            )
            Text(
                text = MakeShieldGuideContent.subtitle,
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = HaoTheme.spacing.sm),
            )
        }

        Text(
            text = MakeShieldGuideContent.physicalStrengthMessage,
            modifier = Modifier
                .padding(top = HaoTheme.spacing.xl)
                .fillMaxWidth()
                .clip(HaoTheme.shapes.panel)
                .background(HaoTheme.colors.stone)
                .padding(HaoTheme.spacing.md),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.ink,
        )

        MakeShieldGuideContent.craftSteps.forEach { step ->
            NumberedStep(number = step.number, title = step.title, body = step.body)
        }

        if (uiState.registeredUid != null) {
            Text(
                text = MakeShieldGuideContent.alreadyRegistered,
                modifier = Modifier.padding(top = HaoTheme.spacing.lg),
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.inkFaint,
            )
        }

        Text(
            text = MakeShieldGuideContent.registerPrompt,
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))

        HaoPrimaryButton(
            text = if (uiState.registeredUid == null) "Register your Shield" else "Register another",
            onClick = onBeginRegistration,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))
    }
}

@Composable
private fun NumberedStep(number: Int, title: String, body: String) {
    Row(modifier = Modifier.padding(top = HaoTheme.spacing.lg)) {
        Text(
            text = number.toString(),
            style = HaoTheme.type.heading,
            color = HaoTheme.colors.inkSoft,
        )
        Spacer(modifier = Modifier.width(HaoTheme.spacing.md))
        Column {
            Text(text = title, style = HaoTheme.type.heading, color = HaoTheme.colors.ink)
            Text(
                text = body,
                modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
            )
        }
    }
}

@Composable
private fun ChooseMethodStep(
    isGeneratingQr: Boolean,
    onChooseNfc: () -> Unit,
    onChooseQr: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenColumn {
        HaoBackLink(onClick = onBack)

        Text(
            text = "Choose your Shield",
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )
        Text(
            text = MakeShieldGuideContent.chooseMethodPrompt,
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))

        HaoPrimaryButton(
            text = MakeShieldGuideContent.methodNfcLabel,
            onClick = onChooseNfc,
            enabled = !isGeneratingQr,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))
        HaoSecondaryButton(
            text = if (isGeneratingQr) "Preparing…" else MakeShieldGuideContent.methodQrLabel,
            onClick = onChooseQr,
            enabled = !isGeneratingQr,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QrDisplayStep(
    uiState: MakeShieldGuideUiState,
    onPrint: () -> Unit,
    onSaveImage: () -> Unit,
    onConfirmScan: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenColumn(horizontalAlignment = Alignment.CenterHorizontally) {
        HaoBackLink(onClick = onBack, modifier = Modifier.align(Alignment.Start))

        Text(
            text = "Your printed Shield",
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = HaoTheme.spacing.sm),
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
        )
        Text(
            text = MakeShieldGuideContent.qrDisplayInstruction,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = HaoTheme.spacing.md),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        uiState.qrBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Your Hǎo Shield QR code",
                modifier = Modifier
                    .padding(top = HaoTheme.spacing.lg)
                    .fillMaxWidth(0.7f)
                    .clip(HaoTheme.shapes.card),
            )
        }

        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))

        HaoPrimaryButton(
            text = "Print on A4",
            onClick = onPrint,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onSaveImage, modifier = Modifier.padding(top = HaoTheme.spacing.xs)) {
            Text(text = "Save as image", style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
        }

        Text(
            text = MakeShieldGuideContent.qrConfirmPrompt,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = HaoTheme.spacing.lg),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))

        HaoSecondaryButton(
            text = "I've printed it — scan to confirm",
            onClick = onConfirmScan,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))
    }
}

@Composable
private fun RegisterShieldStep(
    uiState: MakeShieldGuideUiState,
    onBack: () -> Unit,
) {
    ScreenColumn {
        HaoBackLink(onClick = onBack)

        Text(
            text = "Register your Shield",
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        when {
            !uiState.isNfcAvailable -> Text(
                text = "This device does not support NFC. You can still use a printed code, or " +
                    "Software Mode.",
                modifier = Modifier.padding(top = HaoTheme.spacing.lg),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
            )
            !uiState.isNfcEnabled -> Text(
                text = "Please enable NFC in your device settings, then return here.",
                modifier = Modifier.padding(top = HaoTheme.spacing.lg),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
            )
            else -> {
                Text(
                    text = MakeShieldGuideContent.registerInstruction,
                    modifier = Modifier.padding(top = HaoTheme.spacing.lg),
                    style = HaoTheme.type.body,
                    color = HaoTheme.colors.ink,
                )
                WaitingBreathText(
                    text = "Waiting for your Shield…",
                    modifier = Modifier.padding(top = HaoTheme.spacing.xl),
                )
            }
        }

        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.inkSoft,
            )
        }
    }
}

/** A caption that slowly breathes — the visual signal that the app is listening, live. */
@Composable
private fun WaitingBreathText(text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "waiting")
    val breathAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(HaoMotion.BREATH, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathAlpha",
    )
    Text(
        text = text,
        modifier = modifier.graphicsLayer { alpha = breathAlpha },
        style = HaoTheme.type.caption,
        color = HaoTheme.colors.inkSoft,
    )
}

@Composable
private fun RegistrationSuccessStep(message: String, onContinue: () -> Unit) {
    ScreenColumn {
        Spacer(modifier = Modifier.height(HaoTheme.spacing.xxl))

        // The seal: registration is stamped.
        Text(
            text = "好",
            style = HaoTheme.type.glyphSmall,
            color = HaoTheme.colors.seal,
        )

        Text(
            text = message,
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
        )
        Text(
            text = "Tap or scan your Shield to begin and end protected time in Shield Mode.",
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))

        HaoPrimaryButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
