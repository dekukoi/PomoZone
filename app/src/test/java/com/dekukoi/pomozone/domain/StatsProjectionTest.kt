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
}
