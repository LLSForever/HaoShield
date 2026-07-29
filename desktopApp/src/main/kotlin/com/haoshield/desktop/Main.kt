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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
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
import com.haoshield.desktop.startup.WindowsAutostart
import com.haoshield.domain.model.EndedSessionSummary
import com.haoshield.domain.model.ShieldTokenKind
import com.haoshield.ui.components.HaoPrimaryButton
import com.haoshield.ui.components.HaoSecondaryButton
import com.haoshield.ui.components.HaoTextField
import com.haoshield.ui.components.HaoTextLink
import com.haoshield.ui.protectedscreen.ProtectedTimeFormatter
import com.haoshield.ui.theme.HaoTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun main() = application {
    val graph = remember { DesktopGraph().also(DesktopGraph::start) }
    var windowOpen by remember { mutableStateOf(true) }

    // Closing the window puts the app in the tray rather than ending it: a protected time that
    // stopped because a window was shut would be no protection at all. Quitting outright stays
    // available — this is meant to be friction, not a cage — but it has to be chosen.
    Tray(
        icon = EnsoIcon,
        tooltip = "Hǎo Shield",
        onAction = { windowOpen = true },
        menu = {
            Item("Open", onClick = { windowOpen = true })
            Item("Quit Hǎo Shield", onClick = ::exitApplication)
        },
    )

    if (windowOpen) {
        Window(
            onCloseRequest = { windowOpen = false },
            title = "Hǎo Shield",
            state = rememberWindowState(width = 460.dp, height = 680.dp),
        ) {
            HaoTheme {
                ShieldWindow(graph)
            }
        }
    }
}

/**
 * The ensō — a circle drawn in one breath, left open. The app's mark, and small enough at tray size
 * that anything more detailed would only turn to mud.
 */
private object EnsoIcon : Painter() {
    override val intrinsicSize: Size = Size(64f, 64f)

    override fun DrawScope.onDraw() {
        drawArc(
            color = Color(0xFF1A1A1A),
            startAngle = 25f,
            sweepAngle = 310f,
            useCenter = false,
            style = Stroke(width = size.minDimension * 0.11f, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ShieldWindow(graph: DesktopGraph) {
    val tokens by graph.shieldTokenStore.observeRegisteredTokens().collectAsState(emptyList())
    val session by graph.sessionManager.observeSessionState().collectAsState(initial = null)
    val closed by graph.blocker.closedCount.collectAsState()
    val scope = rememberCoroutineScope()

    var notice by remember { mutableStateOf<String?>(null) }
    var editingBlocklist by remember { mutableStateOf(false) }
    var readingJournal by remember { mutableStateOf(false) }
    var unblocking by remember { mutableStateOf(false) }
    var makingShield by remember { mutableStateOf(false) }
    var reflection by remember { mutableStateOf<EndedSessionSummary?>(null) }

    // A time that has just ended may be worth pausing over. The session rules decide which ones
    // qualify; this only asks once the clock has stopped.
    LaunchedEffect(session) {
        if (session == null) {
            reflection = graph.sessionManager.getLastEndedSession()
        }
    }

    reflection?.let { summary ->
        ReflectionScreen(graph, summary, scope, onDone = { reflection = null })
        return
    }

    if (editingBlocklist) {
        BlocklistScreen(graph, scope, onBack = { editingBlocklist = false })
        return
    }

    if (readingJournal) {
        JournalScreen(graph, onBack = { readingJournal = false })
        return
    }

    if (makingShield) {
        MakeYourShield(
            graph = graph,
            scope = scope,
            onBack = { makingShield = false },
            onNotice = { notice = it },
        )
        return
    }

    val unblockingFor = session?.session?.id
    if (unblocking && unblockingFor != null) {
        UnblockScreen(graph, unblockingFor, scope, onBack = { unblocking = false })
        return
    }

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
            // A running session comes first, always. Anything else on top of it would hide the one
            // thing the window exists to show.
            current == null -> AtRest(
                graph = graph,
                scope = scope,
                hasShield = tokens.isNotEmpty(),
                onMakeShield = { makingShield = true },
                onEditBlocklist = { editingBlocklist = true },
                onReadJournal = { readingJournal = true },
                onNotice = { notice = it },
            )

            else -> InSession(
                graph = graph,
                elapsedMillis = current.elapsedMillis,
                isShieldSession = current.isShieldMode,
                closedCount = closed,
                allowedCount = current.temporarilyAllowedPackages.size,
                intention = current.session.intention,
                scope = scope,
                onUnblock = { unblocking = true },
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
    onBack: () -> Unit,
    onNotice: (String?) -> Unit,
) {
    val payload = remember { ShieldQr.newPayload() }
    var typed by remember { mutableStateOf("") }

    LaunchedEffect(payload) {
        graph.shieldTokenStore.setPendingQrPayload(payload)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(HaoTheme.spacing.screenH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
                if (result is ShieldScanResult.RegistrationComplete) onBack()
            }
        },
        modifier = Modifier.padding(top = HaoTheme.spacing.md),
    )

        HaoTextLink(
            text = "Not now",
            onClick = {
                onNotice(null)
                onBack()
            },
            color = HaoTheme.colors.inkFaint,
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
        )
    }
}

@Composable
private fun AtRest(
    graph: DesktopGraph,
    scope: CoroutineScope,
    hasShield: Boolean,
    onMakeShield: () -> Unit,
    onEditBlocklist: () -> Unit,
    onReadJournal: () -> Unit,
    onNotice: (String?) -> Unit,
) {
    val blocked by graph.blockingRepository.observeBlockedPackageNames()
        .collectAsState(initial = emptySet())

    Text(
        text = "Set this time aside.",
        style = HaoTheme.type.voice,
        color = HaoTheme.colors.inkSoft,
        modifier = Modifier.padding(top = HaoTheme.spacing.lg),
    )

    // An open time asks nothing of anyone. Only Shield Mode needs a Shield, so making one is
    // offered rather than demanded — the app is usable from the first moment it opens.
    if (hasShield) {
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
    } else {
        HaoPrimaryButton(
            text = "Begin",
            onClick = {
                scope.launch {
                    graph.sessionManager.startSession(SessionMode.SOFTWARE)
                    onNotice(null)
                }
            },
            modifier = Modifier.padding(top = HaoTheme.spacing.xl),
        )

        HaoTextLink(
            text = "Make a Shield",
            onClick = onMakeShield,
            color = HaoTheme.colors.inkSoft,
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
        )
    }

    HaoTextLink(
        text = "${blocked.size} apps set aside",
        onClick = onEditBlocklist,
        color = HaoTheme.colors.inkSoft,
        modifier = Modifier.padding(top = HaoTheme.spacing.xl),
    )

    HaoTextLink(
        text = "Journal",
        onClick = onReadJournal,
        color = HaoTheme.colors.inkSoft,
        modifier = Modifier.padding(top = HaoTheme.spacing.md),
    )

    // Offered only where it can be honoured: run from Gradle there is no app to point at, and a
    // switch that silently does nothing is worse than no switch.
    val autostart = remember { WindowsAutostart() }
    if (autostart.isAvailable) {
        var startsWithWindows by remember { mutableStateOf(autostart.isEnabled) }

        HaoTextLink(
            text = if (startsWithWindows) {
                "Starts with Windows"
            } else {
                "Start with Windows"
            },
            onClick = {
                val outcome = if (startsWithWindows) autostart.disable() else autostart.enable()
                outcome
                    .onSuccess { startsWithWindows = autostart.isEnabled }
                    .onFailure { onNotice(it.message) }
            },
            color = if (startsWithWindows) HaoTheme.colors.ink else HaoTheme.colors.inkSoft,
            modifier = Modifier.padding(top = HaoTheme.spacing.md),
        )
    }
}

@Composable
private fun InSession(
    graph: DesktopGraph,
    elapsedMillis: Long,
    isShieldSession: Boolean,
    closedCount: Int,
    allowedCount: Int,
    intention: String?,
    scope: CoroutineScope,
    onUnblock: () -> Unit,
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
        text = when {
            allowedCount > 0 -> "$allowedCount let through for now."
            closedCount == 0 -> "Nothing has interrupted you."
            else -> "$closedCount closed while you worked."
        },
        style = HaoTheme.type.caption,
        color = HaoTheme.colors.inkFaint,
        modifier = Modifier.padding(top = HaoTheme.spacing.md),
    )

    Intention(intention, scope, graph)

    HaoTextLink(
        text = "Let something through",
        onClick = onUnblock,
        color = HaoTheme.colors.inkSoft,
        modifier = Modifier.padding(top = HaoTheme.spacing.lg),
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
 * What this time was set aside for. Offered, never asked for — the session screen is meant to hold
 * one thing at a time, so until someone reaches for it this is a single quiet line.
 *
 * Naming it changes what happens at the end: a time with an intention is always worth pausing over,
 * however short it turned out to be.
 */
@Composable
private fun Intention(intention: String?, scope: CoroutineScope, graph: DesktopGraph) {
    var naming by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    when {
        intention != null -> Text(
            text = intention,
            style = HaoTheme.type.voice,
            color = HaoTheme.colors.inkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HaoTheme.spacing.lg),
        )

        naming -> {
            HaoTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "What is this time for?",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HaoTheme.spacing.lg),
            )

            HaoTextLink(
                text = "Keep",
                onClick = {
                    val named = draft.trim()
                    if (named.isNotEmpty()) {
                        scope.launch { graph.sessionManager.setSessionIntention(named) }
                    }
                    naming = false
                },
                color = HaoTheme.colors.ink,
                modifier = Modifier.padding(top = HaoTheme.spacing.sm),
            )
        }

        else -> HaoTextLink(
            text = "Name what this is for",
            onClick = { naming = true },
            color = HaoTheme.colors.inkFaint,
            modifier = Modifier.padding(top = HaoTheme.spacing.lg),
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
