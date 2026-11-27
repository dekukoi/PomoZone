package com.dekukoi.pomozone.domain

import java.time.DayOfWeek
import java.time.LocalDate

data class DayBarEntry(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val label: String,
    val minutes: Long,
    val isToday: Boolean,
)

data class StatsModel(
    val todayMinutes: Long = 0L,
    val thisWeekMinutes: Long = 0L,
    val allTimeMinutes: Long = 0L,
    val streakDays: Int = 0,
    val totalSessions: Int = 0,
    val todaySessions: Int = 0,
    val dailyGoal: Int = PomodoroConfig.DAILY_GOAL,
    val weeklyBars: List<DayBarEntry> = emptyList(),
) {
    val todayFormatted: String get() = formatDuration(todayMinutes)
    val thisWeekFormatted: String get() = formatDuration(thisWeekMinutes)
    val allTimeFormatted: String get() = formatDuration(allTimeMinutes)
    val streakFormatted: String get() = formatStreak(streakDays)
    val goalLabel: String get() = "$todaySessions of $dailyGoal"
    val goalFraction: Float get() = if (dailyGoal > 0) {
        (todaySessions.toFloat() / dailyGoal).coerceIn(0f, 1f)
    } else {
        0f
    }
}

fun formatDuration(minutes: Long): String {
    if (minutes <= 0L) return "0"
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        (hours > 0) && (mins > 0) -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

fun formatStreak(days: Int): String {
    return if (days == 1) "1 day" else "$days days"
}
