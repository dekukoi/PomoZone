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
    fun `when remaining time reaches zero exactly one session is saved and engine moves to short break idle`() = runTest {
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

        // Ticket 04: completed focus block moves to Short break in the idle state
        val state = engine.state.value
        assertEquals(TimerMode.ShortBreak, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.SHORT_BREAK_DURATION, state.remainingTime)
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

    // ── Ticket 03: pause / resume / reset ────────────────────────────────────

    @Test
    fun `pausing a running block transitions status to paused`() {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()
        engine.pause()
        assertEquals(TimerStatus.Paused, engine.state.value.status)
    }

    @Test
    fun `pause then resume after real time leaves remaining time exactly as it was at pause`() {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()

        // Run for 5 minutes
        clock.advance(Duration.ofMinutes(5))
        engine.pause()
        val remainingAtPause = engine.state.value.remainingTime
        assertEquals(Duration.ofMinutes(20), remainingAtPause)

        // Let real time pass while paused — 10 minutes go by
        clock.advance(Duration.ofMinutes(10))
        engine.resume()

        // After resume the remaining must be exactly 20 minutes, not 10
        assertEquals(Duration.ofMinutes(20), engine.state.value.remainingTime)
        assertEquals(TimerStatus.Running, engine.state.value.status)
    }

    @Test
    fun `pausing saves nothing to the session store`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()
        clock.advance(Duration.ofMinutes(10))
        engine.pause()

        val sessions = sessionStore.sessions().first()
        assertEquals(0, sessions.size)
    }

    @Test
    fun `reset while running returns to full duration idle and saves nothing`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()
        clock.advance(Duration.ofMinutes(10))
        engine.reset()

        val state = engine.state.value
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.FOCUS_DURATION, state.remainingTime)
        assertEquals(0, sessionStore.sessions().first().size)
    }

    @Test
    fun `reset while paused returns to full duration idle and saves nothing`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()
        clock.advance(Duration.ofMinutes(10))
        engine.pause()
        engine.reset()

        val state = engine.state.value
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.FOCUS_DURATION, state.remainingTime)
        assertEquals(0, sessionStore.sessions().first().size)
    }

    @Test
    fun `reset while idle keeps full duration idle and saves nothing`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.reset()

        val state = engine.state.value
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.FOCUS_DURATION, state.remainingTime)
        assertEquals(0, sessionStore.sessions().first().size)
    }

    @Test
    fun `completed block after pause and resume carries the original start instant`() = runTest {
        val originalStart = Instant.parse("2026-01-01T10:00:00Z")
        val testClock = MutableFakeClock(originalStart)
        val store = InMemorySessionStore()
        val engine = TimerEngine(clock = testClock, sessionStore = store)

        engine.start()
        // Advance 5 minutes, then pause
        testClock.advance(Duration.ofMinutes(5))
        engine.pause()

        // 10 minutes of wall time pass while paused
        testClock.advance(Duration.ofMinutes(10))
        engine.resume()

        // Run the remaining 20 minutes to completion
        testClock.advance(Duration.ofMinutes(20))
        engine.tick()

        val sessions = store.sessions().first()
        assertEquals(1, sessions.size)
        assertEquals(originalStart, sessions[0].startInstant)
    }

    // ── Ticket 04: cycle, breaks and skip ─────────────────────────────────────

    @Test
    fun `completed focus block moves to short break in idle state showing 5 minutes`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()

        val state = engine.state.value
        assertEquals(TimerMode.ShortBreak, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.SHORT_BREAK_DURATION, state.remainingTime)
        assertEquals(PomodoroConfig.SHORT_BREAK_DURATION, state.totalDuration)
    }

    @Test
    fun `fourth completed focus block leads to a long break showing 15 minutes`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        repeat(3) {
            engine.start()
            clock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick()
            // skip the break to get back to focus quickly
            engine.skip()
        }
        // 4th focus block
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()

        val state = engine.state.value
        assertEquals(TimerMode.LongBreak, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.LONG_BREAK_DURATION, state.remainingTime)
    }

    @Test
    fun `after long break the next block is focus and cycle count is back at zero`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        repeat(3) {
            engine.start()
            clock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick()
            engine.skip()
        }
        // 4th focus block → long break
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()

        // Complete long break
        engine.start()
        clock.advance(PomodoroConfig.LONG_BREAK_DURATION)
        engine.tick()

        val state = engine.state.value
        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(1, state.sessionInCycle)
    }

    @Test
    fun `after a short break the next block is focus`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()
        // Now in short break idle
        engine.start()
        clock.advance(PomodoroConfig.SHORT_BREAK_DURATION)
        engine.tick()

        val state = engine.state.value
        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
    }

    @Test
    fun `next block after completing never starts by itself and waits in idle`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()

        // Should be idle, not running
        assertEquals(TimerStatus.Idle, engine.state.value.status)
    }

    @Test
    fun `skipping a focus block moves to short break idle and saves nothing`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        // Start and partially run a focus block
        engine.start()
        clock.advance(Duration.ofMinutes(10))
        engine.skip()

        val state = engine.state.value
        assertEquals(TimerMode.ShortBreak, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.SHORT_BREAK_DURATION, state.remainingTime)
        assertEquals(0, sessionStore.sessions().first().size)
    }

    @Test
    fun `skipping a focus block does not count toward the cycle of four`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        // Skip 3 focus blocks (they should not count)
        repeat(3) {
            engine.start()
            engine.skip()
            // skip puts us in short break, skip again to return to focus
            engine.skip()
        }
        // Now complete 3 real focus blocks
        repeat(3) {
            engine.start()
            clock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick()
            engine.skip() // skip the break
        }
        // 4th real focus block — should lead to long break
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()

        assertEquals(TimerMode.LongBreak, engine.state.value.mode)
    }

    @Test
    fun `skipping a short break moves to focus idle without saving and sessionInCycle is preserved`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        // Complete first focus block → short break
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()
        val sessionsAfterFocus = sessionStore.sessions().first().size

        engine.skip() // skip break

        val state = engine.state.value
        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(sessionsAfterFocus, sessionStore.sessions().first().size)
        assertEquals(2, state.sessionInCycle) // second focus block
    }

    @Test
    fun `skipping a long break resets cycle to zero and returns to focus`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        repeat(3) {
            engine.start()
            clock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick()
            engine.skip()
        }
        // 4th focus → long break
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()

        engine.skip() // skip long break

        val state = engine.state.value
        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(1, state.sessionInCycle)
    }

    @Test
    fun `reset leaves the cycle count unchanged`() = runTest {
        val engine = TimerEngine(clock = clock, sessionStore = sessionStore)
        // Complete 2 focus sessions
        repeat(2) {
            engine.start()
            clock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick()
            engine.skip()
        }
        val cycleBeforeReset = engine.state.value.sessionInCycle

        engine.start()
        clock.advance(Duration.ofMinutes(10))
        engine.reset()

        assertEquals(cycleBeforeReset, engine.state.value.sessionInCycle)
        assertEquals(TimerStatus.Idle, engine.state.value.status)
    }

    @Test
    fun `full four-session cycle saves exactly four sessions then opens long break then resets cycle`() = runTest {
        val store = InMemorySessionStore()
        val testClock = MutableFakeClock(Instant.parse("2026-01-01T09:00:00Z"))
        val engine = TimerEngine(clock = testClock, sessionStore = store)

        // Complete 4 focus blocks, skipping each short break
        repeat(3) {
            engine.start()
            testClock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick()
            assertEquals(TimerMode.ShortBreak, engine.state.value.mode)
            engine.skip()
            assertEquals(TimerMode.Focus, engine.state.value.mode)
        }
        // 4th focus
        engine.start()
        testClock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()

        assertEquals(TimerMode.LongBreak, engine.state.value.mode)
        assertEquals(4, store.sessions().first().size)

        // Complete long break
        engine.start()
        testClock.advance(PomodoroConfig.LONG_BREAK_DURATION)
        engine.tick()

        val state = engine.state.value
        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(1, state.sessionInCycle)
        assertEquals(4, store.sessions().first().size) // no extra saves
    }

    // ── Ticket 05: background completion ─────────────────────────────────────

    @Test
    fun `running focus block completed by clock jump saves exactly one session and moves to short break idle`() = runTest {
        val startInstant = Instant.parse("2026-01-01T10:00:00Z")
        val testClock = MutableFakeClock(startInstant)
        val store = InMemorySessionStore()
        val engine = TimerEngine(clock = testClock, sessionStore = store)

        engine.start()
        // Simulate background period: jump 45 minutes ahead (well past 25 minutes)
        testClock.advance(Duration.ofMinutes(45))
        engine.tick()

        val state = engine.state.value
        assertEquals(TimerMode.ShortBreak, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.SHORT_BREAK_DURATION, state.remainingTime)
        assertEquals(PomodoroConfig.SHORT_BREAK_DURATION, state.totalDuration)

        val sessions = store.sessions().first()
        assertEquals(1, sessions.size)
        assertEquals(startInstant, sessions[0].startInstant)
        assertEquals(25L, sessions[0].durationMinutes)
    }

    @Test
    fun `further ticks after clock jump completion save nothing more`() = runTest {
        val startInstant = Instant.parse("2026-01-01T10:00:00Z")
        val testClock = MutableFakeClock(startInstant)
        val store = InMemorySessionStore()
        val engine = TimerEngine(clock = testClock, sessionStore = store)

        engine.start()
        testClock.advance(Duration.ofMinutes(45))
        engine.tick()

        assertEquals(1, store.sessions().first().size)

        // Multiple subsequent updates while in background / idle
        testClock.advance(Duration.ofMinutes(5))
        engine.tick()
        testClock.advance(Duration.ofMinutes(30))
        engine.tick()
        testClock.advance(Duration.ofHours(2))
        engine.tick()

        val sessions = store.sessions().first()
        assertEquals(1, sessions.size)
        assertEquals(TimerStatus.Idle, engine.state.value.status)
    }

    @Test
    fun `short break that ended while backgrounded moves to focus idle with no saved session`() = runTest {
        val startInstant = Instant.parse("2026-01-01T10:00:00Z")
        val testClock = MutableFakeClock(startInstant)
        val store = InMemorySessionStore()
        val engine = TimerEngine(clock = testClock, sessionStore = store)

        // Complete 1 focus block
        engine.start()
        testClock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()
        assertEquals(1, store.sessions().first().size)
        assertEquals(TimerMode.ShortBreak, engine.state.value.mode)

        // Start short break and let it finish in background (jump 20 minutes)
        engine.start()
        testClock.advance(Duration.ofMinutes(20))
        engine.tick()

        val state = engine.state.value
        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.FOCUS_DURATION, state.remainingTime)
        assertEquals(PomodoroConfig.FOCUS_DURATION, state.totalDuration)
        assertEquals(2, state.sessionInCycle)
        // Breaks save no session: count remains 1
        assertEquals(1, store.sessions().first().size)
    }

    @Test
    fun `long break that ended while backgrounded moves to focus idle with cycle reset`() = runTest {
        val startInstant = Instant.parse("2026-01-01T10:00:00Z")
        val testClock = MutableFakeClock(startInstant)
        val store = InMemorySessionStore()
        val engine = TimerEngine(clock = testClock, sessionStore = store)

        // Complete 4 focus blocks
        repeat(3) {
            engine.start()
            testClock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick()
            engine.skip() // skip short break
        }
        engine.start()
        testClock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick()
        assertEquals(TimerMode.LongBreak, engine.state.value.mode)
        assertEquals(4, store.sessions().first().size)

        // Start long break and jump ahead 60 minutes in background
        engine.start()
        testClock.advance(Duration.ofMinutes(60))
        engine.tick()

        val state = engine.state.value
        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(PomodoroConfig.FOCUS_DURATION, state.remainingTime)
        assertEquals(1, state.sessionInCycle)
        // Still exactly 4 sessions saved
        assertEquals(4, store.sessions().first().size)
    }
}
