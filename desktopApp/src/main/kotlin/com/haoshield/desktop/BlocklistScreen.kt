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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.haoshield.desktop.blocking.RunningApps
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.components.HaoSectionLabel
import com.haoshield.ui.components.HaoSelectionDot
import com.haoshield.ui.theme.HaoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * What to set aside. The curated groups, then whatever else is running right now — because the
 * presets are a guess at what pulls at someone, and only they know the real answer.
 */
@Composable
fun BlocklistScreen(
    graph: DesktopGraph,
    scope: CoroutineScope,
    onBack: () -> Unit,
) {
    val blocked by graph.blockingRepository.observeBlockedPackageNames()
        .collectAsState(initial = emptySet())

    val presets = remember { graph.blockingRepository.getPresetGroups() }
    // Read once, on entering: a list that reshuffled while being read would be unusable.
    val running = remember { RunningApps.list() }

    val presetNames = remember(presets) { presets.flatMap { it.packageNames }.toSet() }
    val alsoRunning = remember(running, presetNames) {
        running.filter { candidate -> presetNames.none { it.equals(candidate, ignoreCase = true) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HaoTheme.spacing.screenH),
    ) {
        HaoBackLink(onClick = onBack, label = "Done")

        Text(
            text = "What to set aside",
            style = HaoTheme.type.heading,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.sm),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            presets.forEach { group ->
                item(key = "group-${group.id}") {
                    HaoSectionLabel(text = group.displayName, topDivider = true)
                }

                items(group.packageNames, key = { "preset-$it" }) { name ->
                    AppRow(
                        name = name,
                        selected = blocked.any { it.equals(name, ignoreCase = true) },
                        onToggle = { graph.toggle(scope, name, blocked) },
                    )
                }
            }

            if (alsoRunning.isNotEmpty()) {
                item(key = "running-label") {
                    HaoSectionLabel(text = "Running now", topDivider = true)
                }

                items(alsoRunning, key = { "running-$it" }) { name ->
                    AppRow(
                        name = name,
                        selected = blocked.any { it.equals(name, ignoreCase = true) },
                        onToggle = { graph.toggle(scope, name, blocked) },
                    )
                }
            }

            item(key = "footer") {
                Text(
                    text = "A blocked app is closed outright while a protected time runs — it is " +
                        "not asked to save first. Anything you add here should be something you " +
                        "would not mind losing the moment it closes.",
                    style = HaoTheme.type.caption,
                    color = HaoTheme.colors.inkFaint,
                    modifier = Modifier.padding(
                        top = HaoTheme.spacing.lg,
                        bottom = HaoTheme.spacing.xl,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AppRow(name: String, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = HaoTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = name,
            style = HaoTheme.type.body,
            color = if (selected) HaoTheme.colors.ink else HaoTheme.colors.inkSoft,
        )

        HaoSelectionDot(selected = selected)
    }
}

private fun DesktopGraph.toggle(scope: CoroutineScope, name: String, blocked: Set<String>) {
    scope.launch {
        val already = blocked.firstOrNull { it.equals(name, ignoreCase = true) }
        if (already == null) {
            blockingRepository.addBlockedPackage(name)
        } else {
            blockingRepository.removeBlockedPackage(already)
        }
    }
}
