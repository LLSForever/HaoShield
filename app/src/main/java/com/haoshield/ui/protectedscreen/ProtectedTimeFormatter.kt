package com.haoshield.ui.protectedscreen

internal object ProtectedTimeFormatter {
    fun format(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0L) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}