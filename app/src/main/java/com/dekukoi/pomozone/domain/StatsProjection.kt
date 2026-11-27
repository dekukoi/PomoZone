package com.dekukoi.pomozone.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object StatsProjection {

    fun project(
        sessions: List<SessionRecord>,
        clock: Clock,
        zoneId: ZoneId = clock.zone,
    ): StatsModel = project(
        sessions = sessions,
        currentInstant = clock.instant(),
        zoneId = zoneId,
    )

    fun project(
        sessions: List<SessionRecord>,
        currentInstant: Instant,
        zoneId: ZoneId,
    ): StatsModel {
        if (sessions.isEmpty()) {
            return StatsModel()
        }

        val today: LocalDate = currentInstant.atZone(zoneId).toLocalDate()
        val weekStart: LocalDate = today.minusDays(6)

        var todayMinutes = 0L
        var thisWeekMinutes = 0L
        var allTimeMinutes = 0L
        val activeDays = mutableSetOf<LocalDate>()

        for ((startInstant, duration) in sessions) {
            val sessionDay: LocalDate = startInstant.atZone(zoneId).toLocalDate()

            allTimeMinutes += duration

            if (sessionDay == today) {
                todayMinutes += duration
            }

            if (!sessionDay.isBefore(weekStart) && !sessionDay.isAfter(today)) {
                thisWeekMinutes += duration
            }

            if (duration > 0L) {
                activeDays.add(sessionDay)
            }
        }

        val streakDays = computeStreak(activeDays, today)

        return StatsModel(
            todayMinutes = todayMinutes,
            thisWeekMinutes = thisWeekMinutes,
            allTimeMinutes = allTimeMinutes,
            streakDays = streakDays,
            totalSessions = sessions.size,
        )
    }

    private fun computeStreak(activeDays: Set<LocalDate>, today: LocalDate): Int {
        var checkDate = if (today in activeDays) today else today.minusDays(1)
        var streak = 0
        while (checkDate in activeDays) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        return streak
    }
}
