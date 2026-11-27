package com.dekukoi.pomozone.domain

data class StatsModel(
    val todayMinutes: Long = 0L,
    val thisWeekMinutes: Long = 0L,
    val allTimeMinutes: Long = 0L,
    val streakDays: Int = 0,
    val totalSessions: Int = 0,
) {
    val todayFormatted: String get() = formatDuration(todayMinutes)
    val thisWeekFormatted: String get() = formatDuration(thisWeekMinutes)
    val allTimeFormatted: String get() = formatDuration(allTimeMinutes)
    val streakFormatted: String get() = formatStreak(streakDays)
}

fun formatDuration(minutes: Long): String {
    if (minutes <= 0L) return "0"
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        (hours > 0 && mins > 0) -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

fun formatStreak(days: Int): String {
    return if (days == 1) "1 day" else "$days days"
}
