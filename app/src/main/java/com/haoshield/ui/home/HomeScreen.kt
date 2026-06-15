package com.haoshield.ui.home

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.ui.navigation.Route

private val WarmBackground = Color(0xFFF7F5F0)
private val Sage = Color(0xFF5C6B5C)
private val MutedText = Color(0xFF6B6B68)

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
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 32.dp),
    ) {
        Text(
            text = "Hǎo Shield",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Light),
            color = Sage,
        )

        Text(
            text = "Protect your attention. Return to yourself.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MutedText,
        )

        Spacer(modifier = Modifier.height(40.dp))

        HomeNavButton(
            label = if (uiState.isStartingSoftwareSession) "Starting…" else "Software Mode",
            description = "A lighter way to begin",
            onClick = viewModel::onSoftwareModeClick,
            enabled = !uiState.isStartingSoftwareSession,
        )

        HomeNavButton(
            label = "Shield Mode",
            description = "Use your physical Hǎo Shield",
            onClick = { onNavigate(Route.ShieldMode.path) },
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HomeNavButton(
            label = "Journal",
            description = "Your intentions and reflections",
            onClick = { onNavigate(Route.Journal.path) },
        )

        HomeNavButton(
            label = MakeShieldGuideLabel,
            description = "Craft and register a physical Shield",
            onClick = { onNavigate(Route.Guide.path) },
        )
    }
}

private const val MakeShieldGuideLabel = "Make Your Own Hǎo Shield"

@Composable
private fun HomeNavButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Sage else MutedText,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText.copy(alpha = 0.8f),
            )
        }
    }
}