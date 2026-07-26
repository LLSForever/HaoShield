package com.haoshield.desktop.blocking

import com.haoshield.domain.repository.BlockingRepository
import com.haoshield.domain.service.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.stream.Collectors

/**
 * The desktop's answer to the accessibility service: a watcher that closes blocked apps while a
 * session is running.
 *
 * It asks each running process for its executable name and ends the ones on the blocklist. There is
 * no equivalent of Android's "draw over the app" here, so the gesture is blunter.
 *
 * Be clear about how blunt: on Windows this is a forced termination, not a polite "please close".
 * The process gets no chance to save. That is tolerable for the curated presets — launchers, chat
 * apps, games, which keep their state on a server — and is the reason browsers and anything
 * document-shaped are deliberately not on that list. Anything a person adds themselves is their
 * own judgement to make, and worth warning about at the point they add it.
 *
 * This is honest friction, not a cage: anyone can quit this app and reopen what they blocked. It is
 * meant to interrupt the reach for something, which is the moment that actually matters.
 */
class WindowsAppBlocker(
    private val sessionManager: SessionManager,
    private val blockingRepository: BlockingRepository,
    private val scope: CoroutineScope,
) {
    private val closed = MutableStateFlow(0)

    /** How many windows have been closed this run — the app's only evidence it is doing anything. */
    val closedCount: StateFlow<Int> = closed.asStateFlow()

    private val ownPid = ProcessHandle.current().pid()

    fun start() {
        scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MILLIS)
                runCatching { sweep() }
            }
        }
    }

    private suspend fun sweep() {
        // No session, nothing to guard. Blocking only ever happens inside a protected time.
        sessionManager.getActiveSession() ?: return

        val blocked = blockingRepository.getBlockedPackageNames()
            .map(String::lowercase)
            .toSet()
        if (blocked.isEmpty()) return

        // Drained into a list first: the checks below suspend, and a Java stream's forEach has no
        // way to await them.
        val handles = ProcessHandle.allProcesses().collect(Collectors.toList())

        for (handle in handles) {
            if (handle.pid() == ownPid || !handle.isAlive) continue

            val path = handle.info().command().orElse(null) ?: continue
            val executable = path
                .substringAfterLast('\\')
                .substringAfterLast('/')
                .lowercase()

            if (executable !in blocked) continue
            // Checked here, not only where the list is chosen: a name typed by hand must never be
            // able to point this at something Windows needs to keep running.
            if (ProtectedProcesses.isProtected(executable, path)) continue
            // An app the person consciously unblocked, with a note, is theirs for the window.
            if (sessionManager.isAppTemporarilyAllowed(executable)) continue

            if (handle.destroy()) closed.value += 1
        }
    }

    private companion object {
        // Slow enough to be invisible in the task manager, quick enough that reaching for something
        // blocked doesn't get you a usable window.
        const val POLL_INTERVAL_MILLIS = 2_000L
    }
}
