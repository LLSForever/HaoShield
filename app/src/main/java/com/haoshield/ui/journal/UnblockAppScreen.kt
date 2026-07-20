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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.ui.theme.MutedText
import com.haoshield.ui.theme.Sage
import com.haoshield.ui.theme.WarmBackground

@Composable
fun UnblockAppScreen(
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: UnblockAppViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onCancel) {
            Text(text = "Cancel", color = MutedText)
        }

        Text(
            text = "Unblock for this session",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
            color = Sage,
        )

        Text(
            text = uiState.appLabel,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = "Why do you need this app right now?",
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
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
                    color = MutedText.copy(alpha = 0.5f),
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Sage.copy(alpha = 0.6f),
                unfocusedBorderColor = MutedText.copy(alpha = 0.25f),
                cursorColor = Sage,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            ),
            minLines = 4,
            enabled = uiState.hasActiveSession && !uiState.isSubmitting,
        )

        Text(
            text = "This unblock lasts only for the current session.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MutedText.copy(alpha = 0.75f),
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
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
                style = MaterialTheme.typography.labelLarge,
                color = Sage,
            )
        }
    }
}