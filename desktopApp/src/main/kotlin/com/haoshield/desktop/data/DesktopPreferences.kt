package com.haoshield.desktop.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Properties

/**
 * The desktop's answer to DataStore: a small string map kept in a properties file, held in memory
 * as a [StateFlow] so the same observe-and-react shape works as on Android.
 *
 * Writes are synchronised and rewrite the whole file — this holds a few dozen keys, so the
 * simplicity is worth more than the I/O. Persisting to a temp file and moving it into place keeps a
 * process killed mid-write from leaving a truncated file behind, which matters here: a corrupt file
 * would mean a live session silently disappearing.
 */
class DesktopPreferences(private val file: File) {

    private val state = MutableStateFlow(readFromDisk())

    val values: StateFlow<Map<String, String>> = state.asStateFlow()

    fun get(key: String): String? = state.value[key]

    fun observe(key: String): Flow<String?> = state.map { it[key] }

    @Synchronized
    fun edit(block: (MutableMap<String, String>) -> Unit) {
        val next = state.value.toMutableMap()
        block(next)
        val snapshot = next.toMap()
        state.value = snapshot
        writeToDisk(snapshot)
    }

    private fun readFromDisk(): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return runCatching {
            val properties = Properties()
            file.inputStream().use(properties::load)
            properties.entries.associate { (k, v) -> k.toString() to v.toString() }
        }.getOrDefault(emptyMap())
    }

    private fun writeToDisk(values: Map<String, String>) {
        runCatching {
            file.parentFile?.mkdirs()
            val properties = Properties()
            values.forEach { (k, v) -> properties.setProperty(k, v) }

            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.outputStream().use { properties.store(it, "Hǎo Shield") }
            if (!temp.renameTo(file)) {
                // renameTo will not clobber an existing file on Windows.
                file.delete()
                temp.renameTo(file)
            }
        }
    }

    companion object {
        /**
         * Where the app keeps its things: %LOCALAPPDATA%\HaoShield on Windows, falling back to a
         * dotfolder in the home directory anywhere else.
         */
        fun appDirectory(): File {
            val localAppData = System.getenv("LOCALAPPDATA")
            return if (!localAppData.isNullOrBlank()) {
                File(localAppData, "HaoShield")
            } else {
                File(System.getProperty("user.home"), ".haoshield")
            }
        }
    }
}
