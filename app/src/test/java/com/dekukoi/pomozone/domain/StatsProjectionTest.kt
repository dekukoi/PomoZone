package com.dekukoi.pomozone.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class StatsProjectionTest {

    private val utcZone = ZoneOffset.UTC

    @Test
    fun `with no sessions every card shows zero and total sessions is zero`() {
        val now = Instant.parse("2026-06-15T12:00:00Z")
        val model = StatsProjection.project(
            sessions = emptyList(),
            currentInstant = now,
            zoneId = utcZone,
        )

        assertEquals(0L, model.todayMinutes)
        assertEquals(0L, model.thisWeekMinutes)
        assertEquals(0L, model.allTimeMinutes)
        assertEquals(0, model.streakDays)
        assertEquals(0, model.totalSessions)
        assertEquals("0", model.todayFormatted)
        assertEquals("0", model.thisWeekFormatted)
        assertEquals("0", model.allTimeFormatted)
        assertEquals("0 days", model.streakFormatted)
    }

    @Test
    fun `total number of sessions is available from the stats model`() {
        val now = Instant.parse("2026-06-15T12:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T10:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-14T15:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        assertEquals(3, model.totalSessions)
    }

    @Test
    fun `today focus time sums durations of sessions on the current local day`() {
        val now = Instant.parse("2026-06-15T14:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T08:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T10:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-14T20:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        assertEquals(50L, model.todayMinutes)
        assertEquals("50m", model.todayFormatted)
    }

    @Test
    fun `this week focus time sums durations of sessions in the last 7 days including today`() {
        // Today is June 15. The 7 days are June 9..June 15.
        val now = Instant.parse("2026-06-15T18:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 25L), // today (day 0)
            SessionRecord(Instant.parse("2026-06-14T10:00:00Z"), 25L), // yesterday (day -1)
            SessionRecord(Instant.parse("2026-06-09T11:00:00Z"), 25L), // 6 days ago (day -6) -> included
            SessionRecord(Instant.parse("2026-06-08T22:00:00Z"), 25L), // 7 days ago (day -7) -> excluded
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        assertEquals(75L, model.thisWeekMinutes)
        assertEquals(100L, model.allTimeMinutes)
        assertEquals("1h 15m", model.thisWeekFormatted)
        assertEquals("1h 40m", model.allTimeFormatted)
    }

    @Test
    fun `sessions close to midnight are grouped into the correct local day in a non-UTC time zone`() {
        // Tokyo is UTC+9.
        val tokyo = ZoneId.of("Asia/Tokyo")

        // 2026-06-01T15:30:00Z is 2026-06-02T00:30:00+09:00 (June 2 in Tokyo, 30 min after midnight).
        val sessionNearMidnight = SessionRecord(
            startInstant = Instant.parse("2026-06-01T15:30:00Z"),
            durationMinutes = 25L,
        )

        // Current time in Tokyo: 2026-06-02T01:00:00+09:00 (2026-06-01T16:00:00Z).
        val nowTokyo = Instant.parse("2026-06-01T16:00:00Z")

        val modelTokyo = StatsProjection.project(listOf(sessionNearMidnight), nowTokyo, tokyo)

        // In Tokyo, the session occurred on June 2, which is today!
        assertEquals(25L, modelTokyo.todayMinutes)
        assertEquals(1, modelTokyo.streakDays)

        // In UTC, however, June 2 has not arrived yet (it's 2026-06-01T16:00:00Z, June 1).
        val modelUtc = StatsProjection.project(listOf(sessionNearMidnight), nowTokyo, utcZone)
        assertEquals(25L, modelUtc.todayMinutes) // In UTC, session was June 1, today is June 1
    }

    @Test
    fun `session day grouping across midnight in a negative offset time zone`() {
        // New York is UTC-4 in summer (EDT) or UTC-5 in winter (EST).
        val newYork = ZoneId.of("America/New_York")

        // In winter (EST, UTC-5):
        // 2026-01-02T02:00:00Z is 2026-01-01T21:00:00-05:00 (Jan 1, 9 PM in NY).
        val sessionInNyEvening = SessionRecord(
            startInstant = Instant.parse("2026-01-02T02:00:00Z"),
            durationMinutes = 25L,
        )

        // Current time: 2026-01-02T03:00:00Z (Jan 1, 10 PM in NY).
        val nowNy = Instant.parse("2026-01-02T03:00:00Z")

        val model = StatsProjection.project(listOf(sessionInNyEvening), nowNy, newYork)

        // In New York, local day is Jan 1, matching the session's local day.
        assertEquals(25L, model.todayMinutes)
        assertEquals(1, model.streakDays)
    }

    @Test
    fun `several sessions on one day count as one streak day`() {
        val now = Instant.parse("2026-06-15T20:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T11:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T14:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-14T10:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-14T16:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        // 5 sessions across 2 days -> streak is 2 days.
        assertEquals(2, model.streakDays)
        assertEquals(5, model.totalSessions)
        assertEquals("2 days", model.streakFormatted)
    }

    @Test
    fun `streak continues across consecutive days`() {
        val now = Instant.parse("2026-06-15T12:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 25L), // today
            SessionRecord(Instant.parse("2026-06-14T09:00:00Z"), 25L), // yesterday
            SessionRecord(Instant.parse("2026-06-13T09:00:00Z"), 25L), // 2 days ago
            SessionRecord(Instant.parse("2026-06-12T09:00:00Z"), 25L), // 3 days ago
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        assertEquals(4, model.streakDays)
        assertEquals("4 days", model.streakFormatted)
    }

    @Test
    fun `streak resets after a day with no session`() {
        val now = Instant.parse("2026-06-15T12:00:00Z")
        // Gap on June 14 (yesterday) with no sessions.
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-13T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-12T09:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        // Yesterday had no session and today has no session -> streak is 0.
        assertEquals(0, model.streakDays)
        assertEquals("0 days", model.streakFormatted)
    }

    @Test
    fun `streak resets if there was a gap even when today has a session`() {
        val now = Instant.parse("2026-06-15T12:00:00Z")
        // Session today (June 15), gap on June 14, sessions on June 13 and June 12.
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-13T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-12T09:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        // Gap on June 14 breaks earlier streak, so only today counts.
        assertEquals(1, model.streakDays)
        assertEquals("1 day", model.streakFormatted)
    }

    @Test
    fun `streak continues from yesterday when today has no session yet`() {
        val now = Instant.parse("2026-06-15T12:00:00Z")
        // No session today (June 15), but sessions on June 14, June 13, June 12.
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-14T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-13T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-12T09:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        // Continues from yesterday: 3 days.
        assertEquals(3, model.streakDays)
        assertEquals("3 days", model.streakFormatted)
    }

    @Test
    fun `duration formatting correctly formats minutes and hours and minutes`() {
        assertEquals("0", formatDuration(0L))
        assertEquals("25m", formatDuration(25L))
        assertEquals("1h", formatDuration(60L))
        assertEquals("1h 40m", formatDuration(100L))
        assertEquals("7h 15m", formatDuration(435L))
        assertEquals("42h 5m", formatDuration(2525L))
    }

    @Test
    fun `streak formatting produces correct singular and plural strings`() {
        assertEquals("0 days", formatStreak(0))
        assertEquals("1 day", formatStreak(1))
        assertEquals("5 days", formatStreak(5))
    }

    @Test
    fun `goal progress below the goal shows label and matching fraction`() {
        val now = Instant.parse("2026-06-15T18:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T11:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T14:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-14T10:00:00Z"), 25L), // yesterday
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        assertEquals(3, model.todaySessions)
        assertEquals(4, model.dailyGoal)
        assertEquals("3 of 4", model.goalLabel)
        assertEquals(0.75f, model.goalFraction, 0.001f)
    }

    @Test
    fun `goal progress at the goal shows full ring and matching label`() {
        val now = Instant.parse("2026-06-15T18:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T11:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T14:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T16:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        assertEquals(4, model.todaySessions)
        assertEquals("4 of 4", model.goalLabel)
        assertEquals(1.0f, model.goalFraction, 0.001f)
    }

    @Test
    fun `goal progress above the goal caps ring fraction at full and shows real count in label`() {
        val now = Instant.parse("2026-06-15T20:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T11:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T13:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T15:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-06-15T17:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        assertEquals(5, model.todaySessions)
        assertEquals("5 of 4", model.goalLabel)
        assertEquals(1.0f, model.goalFraction, 0.001f)
    }

    @Test
    fun `with no sessions weekly bars has seven zero-filled entries oldest first and today last`() {
        // Today is Monday June 15, 2026
        val now = Instant.parse("2026-06-15T12:00:00Z")
        val model = StatsProjection.project(emptyList(), now, utcZone)

        val bars = model.weeklyBars
        assertEquals(7, bars.size)

        // 7 days: June 9 (Tuesday) to June 15 (Monday)
        val expectedDates = (6 downTo 0).map { offset ->
            java.time.LocalDate.parse("2026-06-15").minusDays(offset.toLong())
        }
        for (i in 0 until 7) {
            val bar = bars[i]
            assertEquals(expectedDates[i], bar.date)
            assertEquals(0L, bar.minutes)
            assertEquals(expectedDates[i].dayOfWeek.name.first().toString(), bar.label)
            if (i == 6) {
                org.junit.Assert.assertTrue("Today's bar should be flagged as isToday", bar.isToday)
            } else {
                org.junit.Assert.assertFalse("Non-today bar should not be isToday", bar.isToday)
            }
        }
    }

    @Test
    fun `weekly bars has exactly 7 bars with correct minutes and today highlighted`() {
        // Today is Monday June 15, 2026
        val now = Instant.parse("2026-06-15T12:00:00Z")
        val sessions = listOf(
            SessionRecord(Instant.parse("2026-06-15T09:00:00Z"), 50L), // today (June 15): 50m
            SessionRecord(Instant.parse("2026-06-13T10:00:00Z"), 25L), // Saturday June 13: 25m
            SessionRecord(Instant.parse("2026-06-09T08:00:00Z"), 100L), // Tuesday June 9: 100m
            SessionRecord(Instant.parse("2026-06-08T18:00:00Z"), 50L), // June 8: 8 days ago (outside 7-day range)
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        val bars = model.weeklyBars
        assertEquals(7, bars.size)

        // Oldest first: June 9
        assertEquals(java.time.LocalDate.parse("2026-06-09"), bars[0].date)
        assertEquals(100L, bars[0].minutes)
        org.junit.Assert.assertFalse(bars[0].isToday)

        // June 10, 11, 12 should be empty (0m)
        assertEquals(0L, bars[1].minutes)
        assertEquals(0L, bars[2].minutes)
        assertEquals(0L, bars[3].minutes)

        // June 13
        assertEquals(java.time.LocalDate.parse("2026-06-13"), bars[4].date)
        assertEquals(25L, bars[4].minutes)

        // June 14
        assertEquals(0L, bars[5].minutes)

        // Today (June 15): last entry
        assertEquals(java.time.LocalDate.parse("2026-06-15"), bars[6].date)
        assertEquals(50L, bars[6].minutes)
        org.junit.Assert.assertTrue(bars[6].isToday)
    }

    @Test
    fun `heatmap level thresholds cover all boundary values`() {
        assertEquals(0, heatmapLevelFor(0L))
        assertEquals(1, heatmapLevelFor(1L))
        assertEquals(1, heatmapLevelFor(25L))
        assertEquals(2, heatmapLevelFor(26L))
        assertEquals(2, heatmapLevelFor(50L))
        assertEquals(3, heatmapLevelFor(51L))
        assertEquals(3, heatmapLevelFor(100L))
        assertEquals(4, heatmapLevelFor(101L))
    }

    @Test
    fun `with no sessions heatmap has 12 weeks with zero-filled days and blank future days`() {
        // Today is Thursday June 18, 2026
        val now = Instant.parse("2026-06-18T12:00:00Z")
        val today = java.time.LocalDate.parse("2026-06-18")
        val model = StatsProjection.project(emptyList(), now, utcZone)

        val weeks = model.heatmapWeeks
        assertEquals(12, weeks.size)

        for (week in weeks) {
            assertEquals(7, week.days.size)
            assertEquals(java.time.DayOfWeek.MONDAY, week.days.first().dayOfWeek)
            assertEquals(java.time.DayOfWeek.SUNDAY, week.days.last().dayOfWeek)
            for (day in week.days) {
                if (day.date.isAfter(today)) {
                    org.junit.Assert.assertTrue("Day after today should be marked as future", day.isFuture)
                } else {
                    org.junit.Assert.assertFalse("Day on or before today should not be marked as future", day.isFuture)
                    assertEquals(0L, day.minutes)
                    assertEquals(0, day.level)
                }
            }
        }
    }

    @Test
    fun `heatmap week alignment starts on Monday and ends on current week`() {
        // Today is Wednesday Nov 25, 2026
        val now = Instant.parse("2026-11-25T14:00:00Z")
        val model = StatsProjection.project(emptyList(), now, utcZone)

        val weeks = model.heatmapWeeks
        assertEquals(12, weeks.size)

        // Current week's Monday is Nov 23, 2026
        val currentMonday = java.time.LocalDate.parse("2026-11-23")
        assertEquals(currentMonday, weeks.last().days.first().date)
        assertEquals(java.time.DayOfWeek.MONDAY, weeks.last().days.first().dayOfWeek)

        // Oldest week's Monday is 11 weeks before current Monday (Sept 7, 2026)
        val startMonday = currentMonday.minusWeeks(11)
        assertEquals(startMonday, weeks.first().days.first().date)
        assertEquals(java.time.DayOfWeek.MONDAY, weeks.first().days.first().dayOfWeek)

        // In the current week, Thursday, Friday, Saturday, Sunday are after Wednesday (Nov 25)
        val currentWeek = weeks.last()
        org.junit.Assert.assertFalse(currentWeek.days[0].isFuture) // Mon Nov 23
        org.junit.Assert.assertFalse(currentWeek.days[1].isFuture) // Tue Nov 24
        org.junit.Assert.assertFalse(currentWeek.days[2].isFuture) // Wed Nov 25 (today)
        org.junit.Assert.assertTrue(currentWeek.days[3].isFuture)  // Thu Nov 26
        org.junit.Assert.assertTrue(currentWeek.days[4].isFuture)  // Fri Nov 27
        org.junit.Assert.assertTrue(currentWeek.days[5].isFuture)  // Sat Nov 28
        org.junit.Assert.assertTrue(currentWeek.days[6].isFuture)  // Sun Nov 29
    }

    @Test
    fun `heatmap groups sessions by date and assigns correct levels to each day`() {
        // Today is Wednesday Nov 25, 2026
        val now = Instant.parse("2026-11-25T14:00:00Z")
        val sessions = listOf(
            // Monday Nov 23: 25 min -> level 1
            SessionRecord(Instant.parse("2026-11-23T10:00:00Z"), 25L),
            // Tuesday Nov 24: 50 min (two 25m sessions) -> level 2
            SessionRecord(Instant.parse("2026-11-24T09:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-11-24T14:00:00Z"), 25L),
            // Wednesday Nov 25 (today): 100 min (four sessions) -> level 3
            SessionRecord(Instant.parse("2026-11-25T08:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-11-25T10:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-11-25T12:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-11-25T14:00:00Z"), 25L),
            // Last week Friday (Nov 20): 125 min (five sessions) -> level 4
            SessionRecord(Instant.parse("2026-11-20T08:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-11-20T10:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-11-20T12:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-11-20T14:00:00Z"), 25L),
            SessionRecord(Instant.parse("2026-11-20T16:00:00Z"), 25L),
        )

        val model = StatsProjection.project(sessions, now, utcZone)

        val currentWeek = model.heatmapWeeks[11]
        val monday = currentWeek.days[0]
        assertEquals(25L, monday.minutes)
        assertEquals(1, monday.level)

        val tuesday = currentWeek.days[1]
        assertEquals(50L, tuesday.minutes)
        assertEquals(2, tuesday.level)

        val wednesday = currentWeek.days[2]
        assertEquals(100L, wednesday.minutes)
        assertEquals(3, wednesday.level)

        val previousWeek = model.heatmapWeeks[10]
        val friday = previousWeek.days[4] // Friday Nov 20
        assertEquals(125L, friday.minutes)
        assertEquals(4, friday.level)
    }
}
