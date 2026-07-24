package com.haoshield.ui.guide

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.ScanMode
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

    when (uiState.step) {
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
        GuideStep.REGISTER -> {
            DisposableEffect(Unit) {
                viewModel.onEnterRegistrationMode(activity)
                onDispose { viewModel.onLeaveRegistrationMode(activity) }
            }
            RegisterShieldStep(
                uiState = uiState,
                onBack = viewModel::onBackToChooseMethod,
            )
        }
        GuideStep.QR_DISPLAY -> QrDisplayStep(
            uiState = uiState,
            onPrint = { uiState.qrBitmap?.let { ShieldQrPrinter.print(context, it) } },
            onSaveImage = { uiState.qrBitmap?.let { ShieldQrSharing.share(context, it) } },
            onConfirmScan = { onNavigateToScanner(ScanMode.REGISTRATION) },
            onBack = viewModel::onBackToChooseMethod,
        )
        GuideStep.SUCCESS -> RegistrationSuccessStep(
            onContinue = viewModel::onDismissSuccess,
        )
    }
}

@Composable
private fun ScreenColumn(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
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
private fun BackLink(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(top = HaoTheme.spacing.sm)) {
        Text(text = "Back", style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
    }
}

@Composable
private fun GuideContentStep(
    uiState: MakeShieldGuideUiState,
    onNavigateBack: () -> Unit,
    onBeginRegistration: () -> Unit,
) {
    ScreenColumn {
        BackLink(onNavigateBack)

        // Hero.
        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "好",
                style = HaoTheme.type.glyph.copy(fontSize = 72.sp),
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
                color = HaoTheme.colors.inkSoft.copy(alpha = 0.85f),
            )
        }

        Text(
            text = MakeShieldGuideContent.registerPrompt,
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))

        FilledCta(
            text = if (uiState.registeredUid == null) "Register your Shield" else "Register another",
            onClick = onBeginRegistration,
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
private fun FilledCta(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = HaoTheme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = HaoTheme.colors.ink,
            contentColor = HaoTheme.colors.paper,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text, style = HaoTheme.type.body, color = HaoTheme.colors.paper)
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
        BackLink(onBack)

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

        FilledCta(
            text = MakeShieldGuideContent.methodNfcLabel,
            onClick = onChooseNfc,
            enabled = !isGeneratingQr,
        )
        Spacer(modifier = Modifier.height(HaoTheme.spacing.md))
        TextButton(
            onClick = onChooseQr,
            enabled = !isGeneratingQr,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (isGeneratingQr) "Preparing…" else MakeShieldGuideContent.methodQrLabel,
                style = HaoTheme.type.body,
                color = HaoTheme.colors.ink,
            )
        }
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
        BackLink(onBack)

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

        FilledCta(text = "Print on A4", onClick = onPrint)
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

        FilledCta(text = "I've printed it — scan to confirm", onClick = onConfirmScan)

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))
    }
}

@Composable
private fun RegisterShieldStep(
    uiState: MakeShieldGuideUiState,
    onBack: () -> Unit,
) {
    ScreenColumn {
        BackLink(onBack)

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
                Text(
                    text = "Waiting for your Shield…",
                    modifier = Modifier.padding(top = HaoTheme.spacing.xl),
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkSoft.copy(alpha = 0.8f),
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

@Composable
private fun RegistrationSuccessStep(onContinue: () -> Unit) {
    ScreenColumn {
        Spacer(modifier = Modifier.height(HaoTheme.spacing.xxl))

        Text(
            text = MakeShieldGuideContent.registerSuccess,
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

        FilledCta(text = "Continue", onClick = onContinue)
    }
}
