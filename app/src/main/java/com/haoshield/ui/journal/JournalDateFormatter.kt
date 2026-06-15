package com.haoshield.ui.journal

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object JournalDateFormatter {
    private val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

    fun format(epochMillis: Long): String =
        formatter.format(Date(epochMillis))
}