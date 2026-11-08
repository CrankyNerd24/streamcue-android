package com.streamcueapp.streamcue.ui

import java.time.Instant
import java.time.temporal.ChronoUnit

/** "just now", "12 min ago", "3h ago", "2d ago" — used by both detail
 * screens' "Updated X ago" footer. */
fun relativeTime(epochMillis: Long): String {
    val minutes = ChronoUnit.MINUTES.between(Instant.ofEpochMilli(epochMillis), Instant.now())
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}
