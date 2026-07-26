package com.haoshield.desktop.data

import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalEntryType
import com.haoshield.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * The journal, as a plain text file — one entry per line, oldest first.
 *
 * Room's place on the desktop, until there is enough here to want a database. The format is
 * deliberately legible: this is the person's own writing, and they should be able to open the file
 * and read it without the app.
 */
class DesktopJournalRepository(private val file: File) : JournalRepository {

    private val entries = MutableStateFlow(readFromDisk())

    override fun observeEntries(): Flow<List<JournalEntry>> = entries.asStateFlow()

    override suspend fun getEntry(id: Long): JournalEntry? = entries.value.firstOrNull { it.id == id }

    // A mutex rather than @Synchronized: these are suspending, and blocking a coroutine's thread to
    // guard a file write is exactly what a mutex is for.
    private val writeLock = Mutex()

    override suspend fun saveEntry(entry: JournalEntry): JournalEntry = writeLock.withLock {
        val saved = if (entry.id == 0L) {
            entry.copy(id = (entries.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        } else {
            entry
        }
        entries.value = entries.value + saved
        writeToDisk(entries.value)
        saved
    }

    override suspend fun pruneEntriesBefore(cutoffEpochMillis: Long) = writeLock.withLock {
        val kept = entries.value.filter { it.createdAtEpochMillis >= cutoffEpochMillis }
        if (kept.size != entries.value.size) {
            entries.value = kept
            writeToDisk(kept)
        }
    }

    private fun readFromDisk(): List<JournalEntry> {
        if (!file.exists()) return emptyList()
        return runCatching {
            file.readLines().mapNotNull(::decode)
        }.getOrDefault(emptyList())
    }

    private fun writeToDisk(values: List<JournalEntry>) {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(values.joinToString("\n", postfix = "\n", transform = ::encode))
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }

    // id \t createdAt \t sessionId \t type \t unblocked \t content — content last, with its
    // newlines and tabs escaped, so a multi-line reflection still occupies exactly one line.
    private fun encode(entry: JournalEntry): String = listOf(
        entry.id.toString(),
        entry.createdAtEpochMillis.toString(),
        entry.sessionId?.toString().orEmpty(),
        entry.type.name,
        entry.unblockedPackageName.orEmpty(),
        entry.content.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t"),
    ).joinToString("\t")

    private fun decode(line: String): JournalEntry? {
        if (line.isBlank()) return null
        val parts = line.split("\t")
        if (parts.size < 6) return null

        return JournalEntry(
            id = parts[0].toLongOrNull() ?: return null,
            createdAtEpochMillis = parts[1].toLongOrNull() ?: return null,
            sessionId = parts[2].toLongOrNull(),
            type = runCatching { JournalEntryType.valueOf(parts[3]) }
                .getOrDefault(JournalEntryType.REFLECTION),
            unblockedPackageName = parts[4].takeIf(String::isNotBlank),
            content = parts.drop(5).joinToString("\t")
                .replace("\\t", "\t").replace("\\n", "\n").replace("\\\\", "\\"),
        )
    }
}
