package com.haoshield.ui.reflection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoTextField
import com.haoshield.ui.theme.HaoTheme

@Composable
fun ReflectionScreen(
    onDone: () -> Unit,
    viewModel: ReflectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.doneEvents.collect { onDone() }
    }

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
            text = "The session has ended.",
            style = HaoTheme.type.heading,
            color = HaoTheme.colors.ink,
            textAlign = TextAlign.Center,
        )

        Text(
            text = durationLine(uiState.minutesProtected),
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
            style = HaoTheme.type.voice,
            color = HaoTheme.colors.inkSoft,
            textAlign = TextAlign.Center,
        )

        uiState.intention?.let { intention ->
            Text(
                text = "For: $intention",
                modifier = Modifier.padding(top = HaoTheme.spacing.sm),
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.inkFaint,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(HaoTheme.spacing.xl))

        HaoTextField(
            value = uiState.reflectionText,
            onValueChange = viewModel::onReflectionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "How was it? (optional)",
            minLines = 2,
        )

        Spacer(modifier = Modifier.height(HaoTheme.spacing.lg))

        HaoPrimaryButton(
            text = "Done",
            onClick = viewModel::onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun durationLine(minutes: Long): String = when (minutes) {
    0L -> "Less than a minute protected."
    1L -> "1 minute protected."
    else -> "$minutes minutes protected."
}
