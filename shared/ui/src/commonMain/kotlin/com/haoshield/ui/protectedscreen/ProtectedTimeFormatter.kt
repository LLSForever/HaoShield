package com.haoshield.ui.protectedscreen

object ProtectedTimeFormatter {
    fun format(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        // padStart rather than String.format: the latter is JVM-only, and this clock is shared.
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')

        return if (hours > 0L) "$hours:$mm:$ss" else "$mm:$ss"
    }
}