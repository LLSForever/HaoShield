package com.haoshield.ui.journal

import com.haoshield.domain.model.JournalEntry
import com.haoshield.domain.model.JournalEntryType

data class JournalEntryUiModel(
    val id: Long,
    val content: String,
    val formattedDate: String,
    val typeLabel: String,
    val appLabel: String?,
) {
    val isUnblockEntry: Boolean
        get() = appLabel != null
}

fun JournalEntry.toUiModel(appLabel: String? = null): JournalEntryUiModel {
    val resolvedAppLabel = unblockedPackageName?.let { packageName ->
        appLabel ?: packageName
    }

    return JournalEntryUiModel(
        id = id,
        content = content,
        formattedDate = JournalDateFormatter.format(createdAtEpochMillis),
        typeLabel = when (type) {
            JournalEntryType.UNBLOCK -> "Unblock intention"
            JournalEntryType.REFLECTION -> "Reflection"
            JournalEntryType.EMERGENCY_EXIT -> "Ended without Shield"
        },
        appLabel = resolvedAppLabel,
    )
}