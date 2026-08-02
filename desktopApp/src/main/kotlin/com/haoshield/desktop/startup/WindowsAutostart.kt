package com.haoshield.desktop.startup

import java.io.File
import java.util.Locale

/**
 * Starting with Windows.
 *
 * A guard that has to be remembered is not much of a guard: the moment that matters is the one
 * where someone reaches for something, and a reboot should not quietly hand that moment back.
 *
 * Done with a small script in the Startup folder rather than a registry Run key — it is the same
 * mechanism, needs no elevation, and a person can see it, read it, and delete it without this app's
 * help. Anything that starts itself on your machine should be that easy to find.
 */
class WindowsAutostart(
    private val startupDirectory: File = defaultStartupDirectory(),
    private val executablePath: () -> String? = { ProcessHandle.current().info().command().orElse(null) },
) {

    private val script: File get() = File(startupDirectory, SCRIPT_NAME)

    val isEnabled: Boolean get() = script.exists()

    /**
     * Whether this build can start itself at all. Run from Gradle the current process is a bare
     * JVM, and a script pointing at java.exe would start nothing — so it is offered only from the
     * packaged app, and says so rather than writing something that quietly does not work.
     */
    val isAvailable: Boolean
        get() {
            val path = executablePath() ?: return false
            val name = path.substringAfterLast('\\').substringAfterLast('/').lowercase(Locale.ROOT)
            return name !in JVM_EXECUTABLES
        }

    fun enable(): Result<Unit> = runCatching {
        val path = executablePath() ?: error("Cannot tell where this app is running from.")
        check(isAvailable) { "Starting with Windows is available from the installed app." }

        startupDirectory.mkdirs()
        script.writeText(
            """
            @echo off
            rem Starts Hǎo Shield with Windows. Delete this file to stop it.
            start "" "$path"
            """.trimIndent() + System.lineSeparator(),
        )
    }

    fun disable(): Result<Unit> = runCatching {
        if (script.exists() && !script.delete()) {
            error("Could not remove ${script.absolutePath}")
        }
    }

    private companion object {
        const val SCRIPT_NAME = "HaoShield.cmd"

        val JVM_EXECUTABLES = setOf("java.exe", "javaw.exe", "java", "javaw")

        fun defaultStartupDirectory(): File {
            val appData = System.getenv("APPDATA")
                ?: File(System.getProperty("user.home"), "AppData\\Roaming").path
            return File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\Startup")
        }
    }
}
