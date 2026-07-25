package com.haoshield.ui.journal

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object JournalDateFormatter {
    private val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun format(epochMillis: Long): String =
        formatter.format(Date(epochMillis))

    /** A day header, e.g. "July 12, 2025". */
    fun formatDay(epochMillis: Long): String =
        dayFormatter.format(Date(epochMillis))

    /** A time within a session, e.g. "3:45 PM". */
    fun formatTime(epochMillis: Long): String =
        timeFormatter.format(Date(epochMillis))
}