package com.haoshield.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.haoshield.domain.Hao
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.protectedscreen.ProtectedTimeFormatter
import com.haoshield.ui.theme.HaoTheme
import kotlinx.coroutines.launch

fun main() = application {
    val graph = remember { DesktopGraph().also(DesktopGraph::start) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Hǎo Shield",
        state = rememberWindowState(width = 460.dp, height = 620.dp),
    ) {
        HaoTheme {
            ShieldWindow(graph)
        }
    }
}

@Composable
private fun ShieldWindow(graph: DesktopGraph) {
    val state by graph.sessionManager.observeSessionState().collectAsState(initial = null)
    val closed by graph.blocker.closedCount.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HaoTheme.spacing.screenH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = Hao.GOOD.toString(),
            style = HaoTheme.type.glyph,
            color = HaoTheme.colors.ink,
        )

        val session = state

        if (session == null) {
            Text(
                text = "Set this time aside.",
                style = HaoTheme.type.voice,
                color = HaoTheme.colors.inkSoft,
                modifier = Modifier.padding(top = HaoTheme.spacing.lg),
            )

            HaoPrimaryButton(
                text = "Begin",
                onClick = { scope.launch { graph.sessionManager.startSession(SessionMode.SOFTWARE) } },
                modifier = Modifier.padding(top = HaoTheme.spacing.xl),
            )
        } else {
            Text(
                text = ProtectedTimeFormatter.format(session.elapsedMillis),
                style = HaoTheme.type.clock,
                color = HaoTheme.colors.ink,
                modifier = Modifier.padding(top = HaoTheme.spacing.lg),
            )

            Text(
                text = if (closed == 0) {
                    "Nothing has interrupted you."
                } else {
                    "$closed closed while you worked."
                },
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.inkFaint,
                modifier = Modifier.padding(top = HaoTheme.spacing.md),
            )

            HaoPrimaryButton(
                text = "End",
                onClick = {
                    scope.launch { graph.sessionManager.endSession(SessionEndMethod.IN_APP) }
                },
                modifier = Modifier.padding(top = HaoTheme.spacing.xl),
            )
        }
    }
}
