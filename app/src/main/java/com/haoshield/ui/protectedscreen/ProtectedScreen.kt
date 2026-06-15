package com.haoshield.ui.protectedscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.SessionMode

private val Sage = Color(0xFF5C6B5C)
private val MutedText = Color(0xFF6B6B68)
private val WarmBackground = Color(0xFFF7F5F0)

@Composable
fun ProtectedScreen(
    onNavigateHome: () -> Unit,
    viewModel: ProtectedScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProtectedScreenEvent.NavigateHome -> onNavigateHome()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AmbientMusicToggle(
            isPlaying = uiState.isAmbientMusicPlaying,
            onToggle = viewModel::onToggleAmbientMusic,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = uiState.formattedElapsedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                ),
                color = Sage,
            )

            Text(
                text = uiState.protectionMessage,
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedText,
                textAlign = TextAlign.Center,
            )

            AnimatedVisibility(
                visible = uiState.quoteVisible && uiState.currentQuote != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 2_000)),
                exit = fadeOut(animationSpec = tween(durationMillis = 2_000)),
                modifier = Modifier.padding(top = 48.dp),
            ) {
                Text(
                    text = uiState.currentQuote.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 24.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = MutedText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.endSessionHint?.let { hint ->
                Text(
                    text = hint,
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                )
            }

            TextButton(
                onClick = viewModel::onEndSessionClick,
                enabled = !uiState.isEndingSession,
            ) {
                Text(
                    text = if (uiState.isEndingSession) "Ending…" else uiState.endButtonLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Sage,
                )
            }

            if (uiState.sessionMode == SessionMode.SHIELD && uiState.endSessionHint == null) {
                Text(
                    text = "Tap your Hǎo Shield to end this session.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
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
            fontSize = if (isPlaying) 14.sp else 22.sp,
            color = if (isPlaying) Sage else MutedText.copy(alpha = 0.38f),
        )
    }
}