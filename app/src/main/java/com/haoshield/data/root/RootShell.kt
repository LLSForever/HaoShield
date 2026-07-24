package com.haoshield.data.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A minimal root command runner for "Strict blocking" mode.
 *
 * Deliberately dependency-free (no libsu): we only need to run a handful of fixed `pm suspend`
 * commands, so a plain `su` process is simpler and avoids adding a third-party root library.
 *
 * Security: callers must only ever pass fixed command templates with a package name argument —
 * never user-entered text — since these run with root privileges.
 */
@Singleton
class RootShell @Inject constructor() {

    /**
     * Whether a `su` binary exists on the device. This is a passive file check — it does NOT invoke
     * su, so it will not trigger the Magisk/SuperSU grant prompt. Used to decide whether to offer
     * Strict mode in Settings. The actual grant prompt appears the first time [exec] runs.
     */
    fun isRootBinaryPresent(): Boolean = SU_PATHS.any { path ->
        runCatching { File(path).exists() }.getOrDefault(false)
    }

    /**
     * Run [commands] in a single root shell session. Returns true if the shell exited cleanly.
     * The first call on a Magisk device surfaces the user's root-grant prompt.
     */
    suspend fun exec(commands: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (commands.isEmpty()) return@withContext true
        runCatching {
            val process = ProcessBuilder("su")
                .redirectErrorStream(true)
                .start()
            process.outputStream.bufferedWriter().use { writer ->
                for (command in commands) {
                    writer.write(command)
                    writer.newLine()
                }
                writer.write("exit")
                writer.newLine()
                writer.flush()
            }
            // Drain output so the process can finish.
            process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            process.exitValue() == 0
        }.getOrDefault(false)
    }

    private companion object {
        val SU_PATHS = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/vendor/bin/su",
            "/system/sbin/su",
            "/data/adb/magisk",
        )
    }
}
