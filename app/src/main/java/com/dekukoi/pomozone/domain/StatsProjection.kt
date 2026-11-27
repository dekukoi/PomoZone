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
        val today: LocalDate = currentInstant.atZone(zoneId).toLocalDate()
        val weekStart: LocalDate = today.minusDays(6)

        var todayMinutes = 0L
        var todaySessions = 0
        var thisWeekMinutes = 0L
        var allTimeMinutes = 0L
        val activeDays = mutableSetOf<LocalDate>()
        val dayMinutesMap = mutableMapOf<LocalDate, Long>()

        for ((startInstant, duration) in sessions) {
            val sessionDay: LocalDate = startInstant.atZone(zoneId).toLocalDate()

            allTimeMinutes += duration

            if (sessionDay == today) {
                todayMinutes += duration
                todaySessions++
            }

            if (!sessionDay.isBefore(weekStart) && !sessionDay.isAfter(today)) {
                thisWeekMinutes += duration
                dayMinutesMap[sessionDay] = (dayMinutesMap[sessionDay] ?: 0L) + duration
            }

            if (duration > 0L) {
                activeDays.add(sessionDay)
            }
        }

        val streakDays = computeStreak(activeDays, today)

        val weeklyBars = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            DayBarEntry(
                date = day,
                dayOfWeek = day.dayOfWeek,
                label = day.dayOfWeek.name.first().toString(),
                minutes = dayMinutesMap[day] ?: 0L,
                isToday = (day == today),
            )
        }

        return StatsModel(
            todayMinutes = todayMinutes,
            thisWeekMinutes = thisWeekMinutes,
            allTimeMinutes = allTimeMinutes,
            streakDays = streakDays,
            totalSessions = sessions.size,
            todaySessions = todaySessions,
            dailyGoal = PomodoroConfig.DAILY_GOAL,
            weeklyBars = weeklyBars,
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
