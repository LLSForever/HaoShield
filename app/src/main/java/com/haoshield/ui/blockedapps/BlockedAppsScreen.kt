package com.haoshield.ui.blockedapps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.R
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.components.HaoSectionLabel
import com.haoshield.ui.components.HaoSelectionDot
import com.haoshield.ui.theme.HaoTheme

@Composable
fun BlockedAppsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAppPicker: () -> Unit,
    viewModel: BlockedAppsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-read the device on every resume, so an app installed since you last looked shows up.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

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
            text = "Blocked apps",
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )
        Text(
            text = "Choose what a session holds back. Changes apply to your next boundary.",
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
        )

        Text(
            text = "Only apps on this phone are listed. Anything else in these groups stays " +
                "covered, and appears here if you install it.",
            style = HaoTheme.type.caption,
            color = HaoTheme.colors.inkFaint,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        uiState.sections.forEach { section ->
            HaoSectionLabel(section.title.uppercase(), topDivider = true)
            section.rows.forEach { row ->
                AppToggleRow(
                    label = row.label,
                    blocked = row.blocked,
                    onToggle = { viewModel.onToggle(row.packageName, !row.blocked) },
                )
            }
        }

        HaoSectionLabel("ADD", topDivider = true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToAppPicker)
                .padding(vertical = HaoTheme.spacing.sm),
        ) {
            Text(text = "Add another app", style = HaoTheme.type.body, color = HaoTheme.colors.ink)
        }

        if (!uiState.isLoading && uiState.totalBlocked == 0) {
            Text(
                text = "Nothing is blocked right now. Sessions will hold nothing back.",
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.inkFaint,
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
            )
        }

        // The ridgeline at the foot of the page — a boundary drawn in one line, in the same
        // register as the journal's mountains. Stone: furniture, not a picture to look at.
        Image(
            painter = painterResource(R.drawable.ill_div_pines),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = HaoTheme.spacing.xl),
            colorFilter = ColorFilter.tint(HaoTheme.colors.stone),
        )

        Spacer(modifier = Modifier.padding(bottom = HaoTheme.spacing.xxl))
    }
}

@Composable
private fun AppToggleRow(label: String, blocked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = HaoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            HaoSelectionDot(selected = blocked)
        }
        Spacer(modifier = Modifier.width(HaoTheme.spacing.md))
        Text(text = label, style = HaoTheme.type.body, color = HaoTheme.colors.ink)
    }
}
