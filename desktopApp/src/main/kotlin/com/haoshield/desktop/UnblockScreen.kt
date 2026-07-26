package com.haoshield.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haoshield.desktop.blocking.ProcessName
import com.haoshield.domain.model.UnblockPolicy
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoSelectionDot
import com.haoshield.ui.components.HaoTextField
import com.haoshield.ui.theme.HaoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The way past the boundary, which is the point of there being one rather than a wall.
 *
 * Passing through costs a sentence about why, and lasts [UnblockPolicy.windowMinutes] minutes
 * rather than the rest of the session — so it is a renewal of intention, not a toll paid once. The
 * note is not optional and the domain refuses without it; this screen only makes that visible
 * before the refusal rather than after.
 */
@Composable
fun UnblockScreen(
    graph: DesktopGraph,
    sessionId: Long,
    scope: CoroutineScope,
    onBack: () -> Unit,
) {
    val blocked by graph.blockingRepository.observeBlockedPackageNames()
        .collectAsState(initial = emptySet())

    var chosen by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        HaoBackLink(onClick = onBack, label = "Back")

        Text(
            text = "Let something through",
            style = HaoTheme.type.heading,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        Text(
            text = "For ${UnblockPolicy.windowMinutes} minutes, and it costs a sentence about why. " +
                "Then the boundary comes back.",
            style = HaoTheme.type.caption,
            color = HaoTheme.colors.inkFaint,
            modifier = Modifier.padding(top = HaoTheme.spacing.xs),
        )

        LazyColumn(modifier = Modifier.weight(1f).padding(top = HaoTheme.spacing.md)) {
            items(blocked.sorted(), key = { it }) { name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable {
                            chosen = if (chosen == name) null else name
                            problem = null
                        }
                        .padding(vertical = HaoTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = name,
                        style = HaoTheme.type.body,
                        color = if (chosen == name) HaoTheme.colors.ink else HaoTheme.colors.inkSoft,
                    )
                    HaoSelectionDot(selected = chosen == name)
                }
            }
        }

        chosen?.let { name ->
            HaoTextField(
                value = note,
                onValueChange = {
                    note = it
                    problem = null
                },
                placeholder = "What do you need it for?",
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HaoTheme.spacing.md),
            )

            HaoPrimaryButton(
                text = "Let it through",
                enabled = note.isNotBlank(),
                onClick = {
                    scope.launch {
                        val result = graph.blockingRepository.unblockAppForSession(
                            sessionId = sessionId,
                            // Normalised so the watcher recognises what was just allowed.
                            packageName = ProcessName.normalise(name),
                            journalNote = note.trim(),
                        )
                        result
                            .onSuccess { onBack() }
                            .onFailure { problem = it.message ?: "Could not let it through." }
                    }
                },
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
            )
        }

        problem?.let {
            Text(
                text = it,
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.seal,
                modifier = Modifier.padding(
                    top = HaoTheme.spacing.sm,
                    bottom = HaoTheme.spacing.lg,
                ),
            )
        }

        if (problem == null) {
            Text(
                text = " ",
                style = HaoTheme.type.caption,
                modifier = Modifier.padding(bottom = HaoTheme.spacing.lg),
            )
        }
    }
}
