package com.haoshield.ui.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.theme.HaoTheme

@Composable
fun JournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        HaoBackLink(onClick = onNavigateBack)

        Text(
            text = "Journal",
            modifier = Modifier.padding(top = HaoTheme.spacing.sm, bottom = HaoTheme.spacing.sm),
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
        )

        Text(
            text = "A quiet record of your intentions and reflections.",
            modifier = Modifier.padding(bottom = HaoTheme.spacing.lg),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        if (sessions.isEmpty()) {
            JournalEmptyState(modifier = Modifier.weight(1f))
        } else {
            LazyColumn {
                items(sessions, key = { it.key }) { group ->
                    Column(modifier = Modifier.animateItem()) {
                        JournalSession(group = group)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = HaoTheme.spacing.lg),
                            color = HaoTheme.colors.stone,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalSession(group: JournalSessionGroup) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = group.dateLabel,
            style = HaoTheme.type.label,
            color = HaoTheme.colors.inkFaint,
        )
        group.entries.forEach { entry ->
            JournalEntryRow(
                entry = entry,
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun JournalEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "好",
                style = HaoTheme.type.glyphSmall,
                color = HaoTheme.colors.stone,
            )
            Text(
                text = "Nothing here yet.",
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.ink,
            )
            Text(
                text = "Your intentions and reflections, and the notes you write\n" +
                    "to pass a boundary, are kept here.",
                modifier = Modifier.padding(top = HaoTheme.spacing.sm),
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.inkSoft,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun JournalEntryRow(entry: JournalEntryUiModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // One quiet meta line within the day: "3:45 PM · Unblock intention"
        Text(
            text = "${entry.formattedTime} · ${entry.typeLabel}",
            style = HaoTheme.type.label,
            color = HaoTheme.colors.inkFaint,
        )

        if (entry.isUnblockEntry) {
            Text(
                text = entry.appLabel.orEmpty(),
                modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                style = HaoTheme.type.heading,
                color = HaoTheme.colors.ink,
            )
        }

        Text(
            text = entry.content,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.ink,
        )
    }
}
