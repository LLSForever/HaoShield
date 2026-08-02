package com.haoshield.desktop.blocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The blocklist is a list of typed names, so the guard against ending something Windows needs has
 * to hold for names that were never chosen from a menu. These are the cases where getting it wrong
 * costs the person their machine rather than their afternoon.
 */
class ProtectedProcessesTest {

    private val windows = System.getenv("SystemRoot") ?: "C:\\Windows"

    @Test
    fun `the processes Windows cannot lose are refused`() {
        listOf("csrss.exe", "lsass.exe", "wininit.exe", "services.exe", "svchost.exe")
            .forEach { name ->
                assertTrue(
                    "$name must never be ended",
                    ProtectedProcesses.isProtected(name, "$windows\\System32\\$name"),
                )
            }
    }

    @Test
    fun `a critical name is refused even from an unexpected place`() {
        // Same name, somewhere it has no business being. Still refused: the name alone is enough.
        assertTrue(
            ProtectedProcesses.isProtected("lsass.exe", "C:\\Users\\someone\\Downloads\\lsass.exe"),
        )
    }

    @Test
    fun `anything living in the Windows directory is out of reach`() {
        assertTrue(
            ProtectedProcesses.isProtected("something.exe", "$windows\\System32\\something.exe"),
        )
    }

    @Test
    fun `the guard is not case sensitive, because Windows paths are not`() {
        assertTrue(ProtectedProcesses.isProtected("CSRSS.EXE", null))
        assertTrue(
            ProtectedProcesses.isProtected("Thing.exe", windows.uppercase() + "\\System32\\Thing.exe"),
        )
    }

    @Test
    fun `this app will not end itself`() {
        assertTrue(ProtectedProcesses.isProtected("javaw.exe", "C:\\jdk\\bin\\javaw.exe"))
    }

    @Test
    fun `the machine's own defences are not ours to switch off`() {
        assertTrue(ProtectedProcesses.isProtected("MsMpEng.exe", null))
    }

    @Test
    fun `the apps people actually mean to set aside are still reachable`() {
        listOf(
            "Discord.exe" to "C:\\Users\\someone\\AppData\\Local\\Discord\\Discord.exe",
            "steam.exe" to "C:\\Program Files (x86)\\Steam\\steam.exe",
            "Spotify.exe" to "C:\\Users\\someone\\AppData\\Roaming\\Spotify\\Spotify.exe",
        ).forEach { (name, path) ->
            assertFalse("$name should be blockable", ProtectedProcesses.isProtected(name, path))
        }
    }

    @Test
    fun `what is offered to choose from never includes a protected process`() {
        // The real machine's process list, filtered the way the editor filters it.
        val offered = RunningApps.list()

        assertTrue(
            "the running list should find something on a live machine",
            offered.isNotEmpty(),
        )
        offered.forEach { name ->
            assertFalse(
                "$name should not have been offered",
                ProtectedProcesses.isProtected(name, null),
            )
        }
    }
}
