package com.haoshield.ui.journal

import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalEntryType
import com.haoshield.domain.model.JournalRetention

/** One sitting's worth of journal entries, so an intention, its unblocks, and its reflection read
 *  together as a single return to yourself rather than scattered rows. */
data class JournalSessionGroup(
    val key: String,
    val dateLabel: String,
    val latestMillis: Long,
    val entries: List<JournalEntryUiModel>,
)

data class JournalEntryUiModel(
    val id: Long,
    val content: String,
    val formattedTime: String,
    val typeLabel: String,
    val appLabel: String?,
    /** Dims as the entry nears the end of its season. See [JournalRetention]. */
    val alpha: Float,
) {
    val isUnblockEntry: Boolean
        get() = appLabel != null
}

fun JournalEntry.toUiModel(
    appLabel: String? = null,
    nowMillis: Long = System.currentTimeMillis(),
): JournalEntryUiModel {
    val resolvedAppLabel = unblockedPackageName?.let { packageName ->
        appLabel ?: packageName
    }

    return JournalEntryUiModel(
        id = id,
        content = content,
        alpha = JournalRetention.alphaForAge(nowMillis - createdAtEpochMillis),
        formattedTime = JournalDateFormatter.formatTime(createdAtEpochMillis),
        typeLabel = when (type) {
            JournalEntryType.UNBLOCK -> "Unblock intention"
            JournalEntryType.REFLECTION -> "Reflection"
            JournalEntryType.EMERGENCY_EXIT -> "Ended without Shield"
        },
        appLabel = resolvedAppLabel,
    )
}
