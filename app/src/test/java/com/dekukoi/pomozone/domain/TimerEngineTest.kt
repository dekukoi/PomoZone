package com.dekukoi.pomozone.domain

import com.dekukoi.pomozone.data.InMemorySessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class TimerEngineTest {
    private val clock = MutableFakeClock(Instant.parse("2026-01-01T10:00:00Z"))
    private val sessionStore = InMemorySessionStore()

    @Test
    fun `initial state is idle focus mode with 25 minutes remaining`() {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        val state = engine.state.value

        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(Duration.ofMinutes(25), state.remainingTime)
        assertEquals(1, state.sessionInCycle)
    }

    @Test
    fun `after start remaining time is end instant minus current time with no drift on irregular updates`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()

        assertEquals(TimerStatus.Running, engine.state.value.status)
        assertEquals(Duration.ofMinutes(25), engine.state.value.remainingTime)

        // Advance 13 seconds (irregular update)
        clock.advance(Duration.ofSeconds(13))
        engine.tick()

        assertEquals(Duration.ofMinutes(24).plusSeconds(47), engine.state.value.remainingTime)

        // Advance another irregular jump: 7 minutes and 22 seconds
        clock.advance(Duration.ofMinutes(7).plusSeconds(22))
        engine.tick()

        // 25m - 13s - 7m22s = 25m - 7m35s = 17m25s
        assertEquals(Duration.ofMinutes(17).plusSeconds(25), engine.state.value.remainingTime)
    }

    @Test
    fun `when remaining time reaches zero exactly one session is saved and returns to idle focus block`() = runTest {
        val startInstant = Instant.parse("2026-01-01T10:00:00Z")
        val testClock = MutableFakeClock(startInstant)
        val store = InMemorySessionStore()
        val engine = TimerEngine(clock = testClock, sessionStore = store)

        engine.start()
        testClock.advance(Duration.ofMinutes(25))
        engine.tick()

        val sessions = store.sessions().first()
        assertEquals(1, sessions.size)
        assertEquals(startInstant, sessions[0].startInstant)
        assertEquals(25L, sessions[0].durationMinutes)

        // Returned to idle focus block
        val state = engine.state.value
        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(Duration.ofMinutes(25), state.remainingTime)
    }

    @Test
    fun `further time updates after completion never save a second session`() = runTest {
        val startInstant = Instant.parse("2026-01-01T10:00:00Z")
        val testClock = MutableFakeClock(startInstant)
        val store = InMemorySessionStore()
        val engine = TimerEngine(clock = testClock, sessionStore = store)

        engine.start()
        // Jump past end instant (e.g. 30 minutes)
        testClock.advance(Duration.ofMinutes(30))
        engine.tick()

        var sessions = store.sessions().first()
        assertEquals(1, sessions.size)

        // Further ticks and time passing
        testClock.advance(Duration.ofMinutes(10))
        engine.tick()
        testClock.advance(Duration.ofMinutes(25))
        engine.tick()

        sessions = store.sessions().first()
        assertEquals(1, sessions.size)
    }
}
