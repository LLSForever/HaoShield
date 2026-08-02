package com.haoshield.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.components.HaoSectionLabel
import com.haoshield.ui.journal.JournalDateFormatter
import com.haoshield.ui.journal.toUiModel
import com.haoshield.ui.theme.HaoTheme

/**
 * What was written down, newest first, under the day it was written.
 *
 * Entries fade as their season passes — the same retention the phone applies, since the rule lives
 * in the domain and neither app decides it alone. Nothing here is editable: a journal you can go
 * back and tidy is a journal you will argue with.
 */
@Composable
fun JournalScreen(graph: DesktopGraph, onBack: () -> Unit) {
    val entries by graph.journalRepository.observeEntries().collectAsState(initial = emptyList())

    val newestFirst = entries.sortedByDescending { it.createdAtEpochMillis }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        HaoBackLink(onClick = onBack, label = "Done")

        Text(
            text = "Journal",
            style = HaoTheme.type.heading,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        if (newestFirst.isEmpty()) {
            Text(
                text = "Nothing yet.\n\nWhat you write when you unblock something, and what you " +
                    "make of a time once it has ended, is kept here.",
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HaoTheme.spacing.xxl),
            )
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            var lastDay: String? = null

            newestFirst.forEach { entry ->
                val day = JournalDateFormatter.formatDay(entry.createdAtEpochMillis)
                if (day != lastDay) {
                    lastDay = day
                    item(key = "day-$day-${entry.id}") {
                        HaoSectionLabel(text = day, topDivider = true)
                    }
                }

                item(key = "entry-${entry.id}") {
                    val model = entry.toUiModel()

                    Column(modifier = Modifier.padding(top = HaoTheme.spacing.md)) {
                        Text(
                            text = buildString {
                                append(model.formattedTime)
                                append(" · ")
                                append(model.typeLabel)
                                model.appLabel?.let { append(" · $it") }
                            },
                            style = HaoTheme.type.label,
                            color = HaoTheme.colors.inkFaint,
                        )

                        Text(
                            text = model.content,
                            style = HaoTheme.type.body,
                            color = HaoTheme.colors.ink.copy(alpha = model.alpha),
                            modifier = Modifier.padding(top = HaoTheme.spacing.xs),
                        )
                    }
                }
            }

            item(key = "tail") {
                Text(
                    text = " ",
                    style = HaoTheme.type.caption,
                    modifier = Modifier.padding(bottom = HaoTheme.spacing.xl),
                )
            }
        }
    }
}
