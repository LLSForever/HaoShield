package com.haoshield.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.haoshield.R
import com.haoshield.domain.model.BlockingMode
import com.haoshield.domain.model.ThemePreference
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.components.HaoSectionLabel
import com.haoshield.ui.components.HaoSelectionDot
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToIntro: () -> Unit,
    onNavigateToBlockedApps: () -> Unit,
    onLeaveWithoutShield: () -> Unit,
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
        HaoBackLink(onClick = onNavigateBack)

        Text(
            text = "Settings",
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        // --- MODE ---
        HaoSectionLabel("MODE")
        ModeRow(
            title = "Software Mode",
            description = "A lighter way to begin",
            selected = uiState.mode == BlockingMode.SOFTWARE,
            onClick = { viewModel.onSelectMode(BlockingMode.SOFTWARE) },
        )
        ModeRow(
            title = "Shield Mode",
            description = "Tap or scan your Shield to begin",
            selected = uiState.mode == BlockingMode.SHIELD,
            onClick = { viewModel.onSelectMode(BlockingMode.SHIELD) },
        )
        AnimatedVisibility(
            visible = uiState.mode == BlockingMode.SHIELD,
            enter = fadeIn(animationSpec = tween(HaoMotion.STANDARD)) +
                expandVertically(animationSpec = tween(HaoMotion.STANDARD)),
            exit = fadeOut(animationSpec = tween(HaoMotion.STANDARD)) +
                shrinkVertically(animationSpec = tween(HaoMotion.STANDARD)),
        ) {
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
        HaoSectionLabel("BLOCKED APPS", topDivider = true)
        uiState.blockedGroups.forEach { group ->
            InfoRow(
                title = group.name,
                trailing = "${group.count} apps",
            )
        }
        LinkRow(title = "Edit blocked apps", onClick = onNavigateToBlockedApps)

        // --- BLOCKING METHOD ---
        HaoSectionLabel("BLOCKING METHOD", topDivider = true)
        ToggleRow(
            title = "Strict blocking",
            description = if (uiState.rootAvailable) {
                "Truly suspend blocked apps at the system level during a session — they can't open " +
                    "at all. You'll be asked to grant root the first time a session starts. Takes " +
                    "effect from the next session."
            } else {
                "Requires a rooted device. Without root, Hǎo Shield uses the calm cover-screen, " +
                    "which a determined tap can still slip past."
            },
            checked = uiState.strictBlocking,
            onCheckedChange = viewModel::onToggleStrictBlocking,
            enabled = uiState.rootAvailable,
        )

        // --- SESSION ---
        HaoSectionLabel("SESSION", topDivider = true)
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

        // --- APPEARANCE ---
        HaoSectionLabel("APPEARANCE", topDivider = true)
        ModeRow(
            title = "Follow system",
            description = "Match your device's light or dark setting",
            selected = uiState.theme == ThemePreference.SYSTEM,
            onClick = { viewModel.onSelectTheme(ThemePreference.SYSTEM) },
        )
        ModeRow(
            title = "Paper",
            description = "Warm off-white, always",
            selected = uiState.theme == ThemePreference.LIGHT,
            onClick = { viewModel.onSelectTheme(ThemePreference.LIGHT) },
        )
        ModeRow(
            title = "Dusk",
            description = "Warm near-black, always",
            selected = uiState.theme == ThemePreference.DARK,
            onClick = { viewModel.onSelectTheme(ThemePreference.DARK) },
        )

        // --- ABOUT ---
        HaoSectionLabel("ABOUT", topDivider = true)
        LinkRow(title = "Make Your Own Shield", onClick = onNavigateToGuide)
        LinkRow(title = "Permissions", onClick = onNavigateToPermissions)
        LinkRow(title = "Replay introduction", onClick = onNavigateToIntro)

        // The way out, drawn rather than written. It stands at the very foot of the page, unnamed
        // and unexplained; while a Shield session is running it opens, and otherwise it is simply
        // a door in the wall. An emergency exit shouldn't need a signpost — only a door.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HaoTheme.spacing.xxl, bottom = HaoTheme.spacing.xl),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_door_ajar),
                contentDescription = "End the session without your Shield"
                    .takeIf { uiState.canLeaveWithoutShield },
                modifier = Modifier
                    .size(width = DOOR_WIDTH, height = DOOR_HEIGHT)
                    .then(
                        if (uiState.canLeaveWithoutShield) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onLeaveWithoutShield,
                            )
                        } else {
                            Modifier
                        },
                    ),
                colorFilter = ColorFilter.tint(
                    if (uiState.canLeaveWithoutShield) {
                        HaoTheme.colors.inkSoft
                    } else {
                        HaoTheme.colors.stone
                    },
                ),
            )
        }
    }
}

private val DOOR_WIDTH = 34.dp
private val DOOR_HEIGHT = 48.dp

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
        HaoSelectionDot(selected = selected)
        Spacer(modifier = Modifier.width(HaoTheme.spacing.md))
        Column {
            Text(text = title, style = HaoTheme.type.body, color = HaoTheme.colors.ink)
            Text(text = description, style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
        }
    }
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
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HaoTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = HaoTheme.type.body,
                color = if (enabled) HaoTheme.colors.ink else HaoTheme.colors.inkSoft,
            )
            Text(text = description, style = HaoTheme.type.caption, color = HaoTheme.colors.inkSoft)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            // Quieted to the palette so it doesn't shout "Material" next to the bespoke radios.
            colors = SwitchDefaults.colors(
                checkedTrackColor = HaoTheme.colors.ink,
                checkedThumbColor = HaoTheme.colors.paper,
                uncheckedTrackColor = HaoTheme.colors.stone,
                uncheckedThumbColor = HaoTheme.colors.paperRaised,
                uncheckedBorderColor = HaoTheme.colors.inkSoft,
            ),
        )
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = HaoTheme.spacing.sm),
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
