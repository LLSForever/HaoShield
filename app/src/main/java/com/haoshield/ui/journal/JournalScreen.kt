package com.haoshield.ui.journal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.R
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.theme.HaoTheme

@Composable
fun JournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Once there's something written, the view from the empty state's window settles into the
        // corner of the page — the same hand, the same line. Behind the words, never in their way.
        if (sessions.isNotEmpty()) {
            Image(
                painter = painterResource(R.drawable.ill_mountains),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .size(width = MOUNTAIN_WIDTH, height = MOUNTAIN_HEIGHT),
                colorFilter = ColorFilter.tint(HaoTheme.colors.stone),
            )
        }

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
            text = "A quiet record of your intentions and reflections. Words are kept for a " +
                "season, then let go.",
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

private val ILLUSTRATION_SIZE = 180.dp
// The range is drawn wide and low (roughly 2.8:1), so it sits along the foot of the page rather
// than filling a corner.
private val MOUNTAIN_WIDTH = 240.dp
private val MOUNTAIN_HEIGHT = 85.dp

@Composable
private fun JournalEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Decorative, so no contentDescription — TalkBack should read the words, not the view.
            // Tinted rather than baked, so the drawing follows Calm and Dusk.
            Image(
                painter = painterResource(R.drawable.ill_open_window),
                contentDescription = null,
                modifier = Modifier.size(ILLUSTRATION_SIZE),
                colorFilter = ColorFilter.tint(HaoTheme.colors.inkSoft),
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
    // Older words dim towards the end of their season, so their passing is visible rather than
    // silent — one morning they are simply no longer there, and that was never a surprise.
    Column(modifier = modifier.fillMaxWidth().graphicsLayer { alpha = entry.alpha }) {
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
