package com.haoshield.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.haoshield.domain.Hao
import com.haoshield.domain.model.EndedSessionSummary
import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalEntryType
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoTextField
import com.haoshield.ui.components.HaoTextLink
import com.haoshield.ui.protectedscreen.ProtectedTimeFormatter
import com.haoshield.ui.theme.HaoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Offered once, after a time ends, and only when the session was long enough or had an intention
 * to close — that judgement belongs to the session rules and is already made before this appears.
 *
 * "Not now" is a real answer and sits beside the other one, unweighted. A reflection extracted by
 * nagging is not a reflection.
 */
@Composable
fun ReflectionScreen(
    graph: DesktopGraph,
    summary: EndedSessionSummary,
    scope: CoroutineScope,
    onDone: () -> Unit,
) {
    var written by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HaoTheme.spacing.screenH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = Hao.GOOD.toString(),
            style = HaoTheme.type.glyphMark,
            color = HaoTheme.colors.inkSoft,
        )

        Text(
            text = ProtectedTimeFormatter.format(summary.durationMillis),
            style = HaoTheme.type.clock,
            color = HaoTheme.colors.ink,
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
        )

        Text(
            text = summary.intention?.let { "You set this time aside to $it." }
                ?: "That time is yours.",
            style = HaoTheme.type.voice,
            color = HaoTheme.colors.inkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HaoTheme.spacing.lg),
        )

        HaoTextField(
            value = written,
            onValueChange = { written = it },
            placeholder = "What do you make of it?",
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HaoTheme.spacing.xl),
        )

        HaoPrimaryButton(
            text = "Keep this",
            enabled = written.isNotBlank(),
            onClick = {
                val content = written.trim()
                scope.launch {
                    graph.journalRepository.saveEntry(
                        JournalEntry(
                            content = content,
                            createdAtEpochMillis = System.currentTimeMillis(),
                            sessionId = summary.sessionId,
                            type = JournalEntryType.REFLECTION,
                        ),
                    )
                    graph.sessionManager.clearLastEndedSession()
                    onDone()
                }
            },
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
        )

        HaoTextLink(
            text = "Not now",
            onClick = {
                graph.sessionManager.clearLastEndedSession()
                onDone()
            },
            color = HaoTheme.colors.inkSoft,
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
        )
    }
}
