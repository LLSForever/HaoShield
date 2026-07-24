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
     * Run [commands] in a single root shell session. Returns true only if EVERY command succeeded.
     * The first call on a Magisk device surfaces the user's root-grant prompt.
     *
     * Commands are chained with `&&` so the first failure stops the shell; a trailing sentinel echo
     * then only prints when the whole chain succeeded. Without this, `exitValue()` reflected only the
     * last command — a mid-chain `pm suspend` failure would still report success, and its packages
     * would be recorded as suspended when they weren't.
     */
    suspend fun exec(commands: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (commands.isEmpty()) return@withContext true
        runCatching {
            val process = ProcessBuilder("su")
                .redirectErrorStream(true)
                .start()
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(commands.joinToString(separator = " && "))
                writer.write(" && echo $SUCCESS_SENTINEL")
                writer.newLine()
                writer.write("exit")
                writer.newLine()
                writer.flush()
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            process.exitValue() == 0 && output.contains(SUCCESS_SENTINEL)
        }.getOrDefault(false)
    }

    private companion object {
        const val SUCCESS_SENTINEL = "__HAO_OK__"

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
