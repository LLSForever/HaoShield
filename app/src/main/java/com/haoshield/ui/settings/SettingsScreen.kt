package com.haoshield.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.domain.model.BlockingMode
import com.haoshield.ui.theme.HaoTheme

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        ) {
            Text(text = "Back", style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
        }

        Text(
            text = "Settings",
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        // --- MODE ---
        SectionLabel("MODE")
        ModeRow(
            title = "Software Mode",
            description = "A lighter way to begin",
            selected = uiState.mode == BlockingMode.SOFTWARE,
            onClick = { viewModel.onSelectMode(BlockingMode.SOFTWARE) },
        )
        ModeRow(
            title = "Hǎo Shield Mode",
            description = "Tap or scan your Shield to begin",
            selected = uiState.mode == BlockingMode.SHIELD,
            onClick = { viewModel.onSelectMode(BlockingMode.SHIELD) },
        )
        if (uiState.mode == BlockingMode.SHIELD) {
            TextButton(
                onClick = onNavigateToGuide,
                modifier = Modifier.padding(start = HaoTheme.spacing.xl),
            ) {
                Text(
                    text = if (uiState.hasAnyToken) {
                        buildString {
                            append(registeredSummary(uiState.hasNfcToken, uiState.hasQrToken))
                            append(" · Make another")
                        }
                    } else {
                        "Register your Shield"
                    },
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.ink,
                )
            }
        }

        // --- BLOCKED APPS ---
        SectionLabel("BLOCKED APPS")
        uiState.blockedGroups.forEach { group ->
            InfoRow(
                title = group.name,
                trailing = "${group.count} apps",
            )
        }
        Text(
            text = "Editing these groups is coming soon.",
            style = HaoTheme.type.caption,
            color = HaoTheme.colors.inkSoft.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        // --- SESSION ---
        SectionLabel("SESSION")
        ToggleRow(
            title = "Ambient sound",
            description = "Soft background tones during a session",
            checked = uiState.ambientSound,
            onCheckedChange = viewModel::onToggleAmbientSound,
        )
        ToggleRow(
            title = "Contemplative quotes",
            description = "Occasional words to return to",
            checked = uiState.quotes,
            onCheckedChange = viewModel::onToggleQuotes,
        )

        // --- ABOUT ---
        SectionLabel("ABOUT")
        LinkRow(title = "Make Your Own Shield", onClick = onNavigateToGuide)
        LinkRow(title = "Permissions", onClick = onNavigateToPermissions)

        Spacer(modifier = Modifier.padding(bottom = HaoTheme.spacing.xxl))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = HaoTheme.type.label,
        color = HaoTheme.colors.inkSoft,
        modifier = Modifier.padding(top = HaoTheme.spacing.xl, bottom = HaoTheme.spacing.sm),
    )
}

@Composable
private fun ModeRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = HaoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionDot(selected = selected)
        Spacer(modifier = Modifier.width(HaoTheme.spacing.md))
        Column {
            Text(text = title, style = HaoTheme.type.body, color = HaoTheme.colors.ink)
            Text(text = description, style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
        }
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    val ring = HaoTheme.colors.inkSoft
    val fill = HaoTheme.colors.ink
    Spacer(
        modifier = Modifier
            .size(18.dp)
            .drawBehind {
                drawCircle(color = ring, radius = size.minDimension / 2f, style = Stroke(1.5.dp.toPx()))
                if (selected) {
                    drawCircle(color = fill, radius = size.minDimension / 4f)
                }
            },
    )
}

@Composable
private fun InfoRow(title: String, trailing: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HaoTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = HaoTheme.type.body, color = HaoTheme.colors.ink)
        Text(text = trailing, style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HaoTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = HaoTheme.type.body, color = HaoTheme.colors.ink)
            Text(text = description, style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = HaoTheme.spacing.md),
    ) {
        Text(text = title, style = HaoTheme.type.body, color = HaoTheme.colors.ink)
    }
}

private fun registeredSummary(hasNfc: Boolean, hasQr: Boolean): String = when {
    hasNfc && hasQr -> "NFC + printed registered"
    hasNfc -> "NFC tag registered"
    hasQr -> "Printed code registered"
    else -> "Registered"
}
