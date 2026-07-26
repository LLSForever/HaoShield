package com.haoshield.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.haoshield.data.qr.ShieldQr
import com.haoshield.domain.Hao
import com.haoshield.domain.model.ScanMode
import com.haoshield.domain.model.SessionEndMethod
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.domain.model.ShieldToken
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoSecondaryButton
import com.haoshield.ui.components.HaoTextField
import com.haoshield.ui.protectedscreen.ProtectedTimeFormatter
import com.haoshield.ui.theme.HaoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun main() = application {
    val graph = remember { DesktopGraph().also(DesktopGraph::start) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Hǎo Shield",
        state = rememberWindowState(width = 460.dp, height = 660.dp),
    ) {
        HaoTheme {
            ShieldWindow(graph)
        }
    }
}

@Composable
private fun ShieldWindow(graph: DesktopGraph) {
    val tokens by graph.shieldTokenStore.observeRegisteredTokens().collectAsState(emptyList())
    val session by graph.sessionManager.observeSessionState().collectAsState(initial = null)
    val closed by graph.blocker.closedCount.collectAsState()
    val scope = rememberCoroutineScope()

    var notice by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HaoTheme.spacing.screenH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = Hao.GOOD.toString(),
            style = HaoTheme.type.glyphSmall,
            color = HaoTheme.colors.ink,
        )

        val current = session

        when {
            tokens.isEmpty() -> MakeYourShield(graph, scope) { notice = it }

            current == null -> AtRest(graph, scope) { notice = it }

            else -> InSession(
                graph = graph,
                elapsedMillis = current.elapsedMillis,
                isShieldSession = current.isShieldMode,
                closedCount = closed,
                scope = scope,
                onNotice = { notice = it },
            )
        }

        notice?.let { text ->
            Text(
                text = text,
                style = HaoTheme.type.caption,
                color = HaoTheme.colors.seal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HaoTheme.spacing.lg),
            )
        }
    }
}

/**
 * There is no camera here, so a printed card cannot be read. Instead the desktop makes a Shield of
 * its own: it shows the code once and asks for it back, which only someone who actually wrote it
 * down can do. What is written down becomes the physical object — the same bargain as the printed
 * card, kept by hand rather than by a lens.
 */
@Composable
private fun MakeYourShield(
    graph: DesktopGraph,
    scope: CoroutineScope,
    onNotice: (String?) -> Unit,
) {
    val payload = remember { ShieldQr.newPayload() }
    var typed by remember { mutableStateOf("") }

    LaunchedEffect(payload) {
        graph.shieldTokenStore.setPendingQrPayload(payload)
    }

    Text(
        text = "Write this down, then type it back.",
        style = HaoTheme.type.voice,
        color = HaoTheme.colors.inkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = HaoTheme.spacing.lg),
    )

    Text(
        text = payload.removePrefix(ShieldQr.PREFIX),
        style = HaoTheme.type.body,
        color = HaoTheme.colors.ink,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HaoTheme.spacing.md),
    )

    Text(
        text = "Keep the paper somewhere you must get up to reach. " +
            "Ending a protected time will ask for it.",
        style = HaoTheme.type.caption,
        color = HaoTheme.colors.inkFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HaoTheme.spacing.md),
    )

    HaoTextField(
        value = typed,
        onValueChange = { typed = it },
        placeholder = "The code you just wrote",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HaoTheme.spacing.lg),
    )

    HaoPrimaryButton(
        text = "This is my Shield",
        enabled = typed.isNotBlank(),
        onClick = {
            scope.launch {
                val result = graph.shieldScanHandler.handleScan(
                    token = ShieldToken(ShieldTokenKind.QR, ShieldQr.PREFIX + typed.trim()),
                    mode = ScanMode.REGISTRATION,
                )
                onNotice(result.describe())
            }
        },
        modifier = Modifier.padding(top = HaoTheme.spacing.md),
    )
}

@Composable
private fun AtRest(graph: DesktopGraph, scope: CoroutineScope, onNotice: (String?) -> Unit) {
    Text(
        text = "Set this time aside.",
        style = HaoTheme.type.voice,
        color = HaoTheme.colors.inkSoft,
        modifier = Modifier.padding(top = HaoTheme.spacing.lg),
    )

    HaoPrimaryButton(
        text = "Begin, with your Shield",
        onClick = {
            scope.launch {
                onNotice(graph.presentShield(SHIELD_START_PLACEHOLDER).describe())
            }
        },
        modifier = Modifier.padding(top = HaoTheme.spacing.xl),
    )

    HaoSecondaryButton(
        text = "Begin, openly",
        onClick = {
            scope.launch {
                graph.sessionManager.startSession(SessionMode.SOFTWARE)
                onNotice(null)
            }
        },
        modifier = Modifier.padding(top = HaoTheme.spacing.sm),
    )
}

@Composable
private fun InSession(
    graph: DesktopGraph,
    elapsedMillis: Long,
    isShieldSession: Boolean,
    closedCount: Int,
    scope: CoroutineScope,
    onNotice: (String?) -> Unit,
) {
    var typed by remember { mutableStateOf("") }

    Text(
        text = ProtectedTimeFormatter.format(elapsedMillis),
        style = HaoTheme.type.clock,
        color = HaoTheme.colors.ink,
        modifier = Modifier.padding(top = HaoTheme.spacing.lg),
    )

    Text(
        text = if (closedCount == 0) {
            "Nothing has interrupted you."
        } else {
            "$closedCount closed while you worked."
        },
        style = HaoTheme.type.caption,
        color = HaoTheme.colors.inkFaint,
        modifier = Modifier.padding(top = HaoTheme.spacing.md),
    )

    if (isShieldSession) {
        HaoTextField(
            value = typed,
            onValueChange = { typed = it },
            placeholder = "Your Shield code",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HaoTheme.spacing.xl),
        )

        HaoPrimaryButton(
            text = "End with your Shield",
            enabled = typed.isNotBlank(),
            onClick = {
                scope.launch {
                    val result = graph.presentShield(typed.trim())
                    typed = ""
                    onNotice(result.describe())
                }
            },
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
        )
    } else {
        HaoPrimaryButton(
            text = "End",
            onClick = {
                scope.launch {
                    graph.sessionManager.endSession(SessionEndMethod.IN_APP)
                    onNotice(null)
                }
            },
            modifier = Modifier.padding(top = HaoTheme.spacing.xl),
        )
    }
}

/**
 * Starting a Shield session needs a Shield in hand just as ending one does, so the registered code
 * is used directly rather than asking the person to type it twice in a row.
 */
private const val SHIELD_START_PLACEHOLDER = ""

private suspend fun DesktopGraph.presentShield(code: String): ShieldScanResult {
    val id = if (code.isBlank()) {
        shieldTokenStore.getRegisteredTokens()
            .firstOrNull { it.kind == ShieldTokenKind.QR }
            ?.id
            ?: return ShieldScanResult.NoShieldRegistered
    } else {
        ShieldQr.PREFIX + code
    }

    return shieldScanHandler.handleScan(ShieldToken(ShieldTokenKind.QR, id), ScanMode.SESSION)
}

private fun ShieldScanResult.describe(): String? = when (this) {
    is ShieldScanResult.RegistrationComplete -> "Your Shield is registered."
    is ShieldScanResult.SessionStarted -> null
    is ShieldScanResult.SessionEnded -> null
    is ShieldScanResult.InvalidShield -> "That is not your Shield."
    ShieldScanResult.NoShieldRegistered -> "No Shield registered yet."
    ShieldScanResult.SoftwareSessionActive -> "An open session is already running."
    is ShieldScanResult.Failed -> reason
}
