package com.haoshield.desktop.startup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Every case here runs against a temporary folder, never the real Startup directory — a test suite
 * that arranged for something to launch on the developer's machine would be a poor citizen.
 */
class WindowsAutostartTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val installedApp = "C:\\Program Files\\Hao Shield\\HaoShield.exe"

    private fun autostart(
        directory: File = folder.root,
        executable: String? = installedApp,
    ) = WindowsAutostart(directory) { executable }

    @Test
    fun `turning it on leaves something that starts the app`() {
        val autostart = autostart()

        assertTrue(autostart.enable().isSuccess)

        assertTrue(autostart.isEnabled)
        val script = File(folder.root, "HaoShield.cmd")
        assertTrue(script.exists())
        assertTrue(
            "the script must actually point at the app",
            script.readText().contains(installedApp),
        )
    }

    @Test
    fun `turning it off leaves nothing behind`() {
        val autostart = autostart()
        autostart.enable()

        assertTrue(autostart.disable().isSuccess)

        assertFalse(autostart.isEnabled)
        assertFalse(File(folder.root, "HaoShield.cmd").exists())
    }

    @Test
    fun `turning it off when it was never on is not an error`() {
        assertTrue(autostart().disable().isSuccess)
    }

    @Test
    fun `asking twice is the same as asking once`() {
        val autostart = autostart()

        autostart.enable()
        autostart.enable()

        assertEquals(
            "one script, not two",
            1,
            folder.root.listFiles().orEmpty().count { it.name == "HaoShield.cmd" },
        )
        assertTrue(autostart.isEnabled)
    }

    @Test
    fun `a script is never written that would start nothing`() {
        // Run from Gradle, the current process is a bare JVM.
        val fromGradle = autostart(executable = "C:\\jdk\\bin\\javaw.exe")

        assertFalse("must not be offered", fromGradle.isAvailable)
        assertTrue("and must refuse rather than write a dud", fromGradle.enable().isFailure)
        assertFalse(fromGradle.isEnabled)
    }

    @Test
    fun `the installed app can start itself`() {
        assertTrue(autostart().isAvailable)
    }

    @Test
    fun `a missing startup folder is created rather than failing`() {
        val nested = File(folder.root, "Start Menu\\Programs\\Startup")

        assertTrue(autostart(directory = nested).enable().isSuccess)
        assertTrue(File(nested, "HaoShield.cmd").exists())
    }

    private fun assertEquals(message: String, expected: Int, actual: Int) =
        org.junit.Assert.assertEquals(message, expected.toLong(), actual.toLong())
}
