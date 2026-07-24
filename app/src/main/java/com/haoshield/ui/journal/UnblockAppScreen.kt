package com.haoshield.ui.journal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoTextField
import com.haoshield.ui.theme.HaoTheme

@Composable
fun UnblockAppScreen(
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: UnblockAppViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            // The intention is written and the app is allowed for this session — take the user
            // straight into it, so unblocking feels like opening the app, not filling a form.
            val launchIntent = context.packageManager
                .getLaunchIntentForPackage(uiState.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        HaoBackLink(onClick = onCancel, label = "Cancel")

        Text(
            text = "Unblock for this session",
            style = HaoTheme.type.heading,
            color = HaoTheme.colors.ink,
        )

        Text(
            text = uiState.appLabel,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
        )

        Text(
            text = "Why do you need this app right now?",
            modifier = Modifier.padding(top = HaoTheme.spacing.lg, bottom = HaoTheme.spacing.md),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        HaoTextField(
            value = uiState.intentionNote,
            onValueChange = viewModel::onIntentionNoteChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "A short, honest intention…",
            minLines = 4,
            enabled = uiState.hasActiveSession && !uiState.isSubmitting,
        )

        Text(
            text = "This unblock lasts only for the current session.",
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
            style = HaoTheme.type.caption,
            color = HaoTheme.colors.inkFaint,
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.ink,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HaoPrimaryButton(
            text = if (uiState.isSubmitting) "Saving…" else "Unblock with intention",
            onClick = viewModel::onSubmitUnblock,
            enabled = uiState.hasActiveSession && !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}