package com.haoshield.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haoshield.ui.theme.HaoTheme

@Composable
fun JournalScreen(
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HaoTheme.colors.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Text(
            text = "Journal",
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
        )

        Text(
            text = "A quiet record of your intentions and reflections.",
            modifier = Modifier.padding(bottom = 24.dp),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        if (entries.isEmpty()) {
            Text(
                text = "Your entries will appear here.",
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft.copy(alpha = 0.8f),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    JournalEntryRow(entry = entry)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = HaoTheme.colors.inkSoft.copy(alpha = 0.15f),
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalEntryRow(entry: JournalEntryUiModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = entry.formattedDate,
            style = HaoTheme.type.label,
            color = HaoTheme.colors.inkSoft.copy(alpha = 0.75f),
        )

        if (entry.isUnblockEntry) {
            Text(
                text = entry.appLabel.orEmpty(),
                modifier = Modifier.padding(top = 6.dp),
                style = HaoTheme.type.heading,
                color = HaoTheme.colors.ink,
            )
            Text(
                text = entry.typeLabel,
                modifier = Modifier.padding(top = 2.dp),
                style = HaoTheme.type.label,
                color = HaoTheme.colors.inkSoft.copy(alpha = 0.7f),
            )
        } else {
            Text(
                text = entry.typeLabel,
                modifier = Modifier.padding(top = 6.dp),
                style = HaoTheme.type.label,
                color = HaoTheme.colors.inkSoft.copy(alpha = 0.7f),
            )
        }

        Text(
            text = entry.content,
            modifier = Modifier.padding(top = 10.dp),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.ink,
        )
    }
}