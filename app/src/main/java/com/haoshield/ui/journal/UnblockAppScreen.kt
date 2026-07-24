package com.haoshield.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            .background(HaoTheme.colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onCancel) {
            Text(text = "Cancel", color = HaoTheme.colors.inkSoft)
        }

        Text(
            text = "Unblock for this session",
            style = HaoTheme.type.heading,
            color = HaoTheme.colors.ink,
        )

        Text(
            text = uiState.appLabel,
            modifier = Modifier.padding(top = 8.dp),
            style = HaoTheme.type.heading,
            color = HaoTheme.colors.ink,
        )

        Text(
            text = "Why do you need this app right now?",
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        OutlinedTextField(
            value = uiState.intentionNote,
            onValueChange = viewModel::onIntentionNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            placeholder = {
                Text(
                    text = "A short, honest intention…",
                    color = HaoTheme.colors.inkSoft.copy(alpha = 0.5f),
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HaoTheme.colors.ink.copy(alpha = 0.6f),
                unfocusedBorderColor = HaoTheme.colors.inkSoft.copy(alpha = 0.25f),
                cursorColor = HaoTheme.colors.ink,
                focusedTextColor = HaoTheme.colors.ink,
                unfocusedTextColor = HaoTheme.colors.ink,
            ),
            minLines = 4,
            enabled = uiState.hasActiveSession && !uiState.isSubmitting,
        )

        Text(
            text = "This unblock lasts only for the current session.",
            modifier = Modifier.padding(top = 12.dp),
            style = HaoTheme.type.caption,
            color = HaoTheme.colors.inkSoft.copy(alpha = 0.75f),
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(top = 12.dp),
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.inkSoft,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = viewModel::onSubmitUnblock,
            enabled = uiState.hasActiveSession && !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (uiState.isSubmitting) "Saving…" else "Unblock with intention",
                style = HaoTheme.type.label,
                color = HaoTheme.colors.ink,
            )
        }
    }
}