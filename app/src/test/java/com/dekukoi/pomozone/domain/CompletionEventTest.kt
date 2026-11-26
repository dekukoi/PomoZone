package com.dekukoi.pomozone.domain

import com.dekukoi.pomozone.data.InMemorySessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Engine-surface tests confirming that completionEvents emits exactly once per
 * completed block and carries the correct finished / next-mode pair.
 * Ticket 06: Completion alert.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompletionEventTest {

    private fun makeEngine(
        startInstant: Instant = Instant.parse("2026-01-01T10:00:00Z"),
    ): Triple<MutableFakeClock, InMemorySessionStore, TimerEngine> {
        val clock = MutableFakeClock(startInstant)
        val store = InMemorySessionStore()
        val engine = TimerEngine(clock = clock, sessionStore = store)
        return Triple(clock, store, engine)
    }

    @Test
    fun `completing a focus block emits exactly one completion event`() = runTest {
        val (clock, _, engine) = makeEngine()
        val events = mutableListOf<CompletionEvent>()
        // Start collecting first, then yield so the collector is subscribed before tick emits.
        val job = launch { engine.completionEvents.collect { events.add(it) } }
        advanceUntilIdle()

        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick(notify = true)

        // Multiple further ticks must not emit additional events
        clock.advance(PomodoroConfig.SHORT_BREAK_DURATION)
        engine.tick(notify = true)
        clock.advance(PomodoroConfig.SHORT_BREAK_DURATION)
        engine.tick(notify = true)

        advanceUntilIdle()
        job.cancel()
        assertEquals(1, events.size)
        assertEquals(TimerMode.Focus, events[0].finishedMode)
        assertEquals(TimerMode.ShortBreak, events[0].nextMode)
    }

    @Test
    fun `completing a break block emits exactly one completion event`() = runTest {
        val (clock, _, engine) = makeEngine()
        val events = mutableListOf<CompletionEvent>()
        val job = launch { engine.completionEvents.collect { events.add(it) } }
        advanceUntilIdle()

        // Complete a focus block to reach short break
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick(notify = true)

        // Now complete the short break
        engine.start()
        clock.advance(PomodoroConfig.SHORT_BREAK_DURATION)
        engine.tick(notify = true)

        // Extra idle ticks after the break completes should produce no more events
        engine.tick(notify = true)

        advanceUntilIdle()
        job.cancel()
        assertEquals(2, events.size)
        assertEquals(TimerMode.ShortBreak, events[1].finishedMode)
        assertEquals(TimerMode.Focus, events[1].nextMode)
    }

    @Test
    fun `tick with notify false does not emit a completion event`() = runTest {
        val (clock, _, engine) = makeEngine()
        val events = mutableListOf<CompletionEvent>()
        val job = launch { engine.completionEvents.collect { events.add(it) } }
        advanceUntilIdle()

        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        // Simulate the resume-refresh path (background completion)
        engine.tick(notify = false)

        advanceUntilIdle()
        job.cancel()
        assertEquals(0, events.size)
    }

    @Test
    fun `fourth focus block completion event carries long break as next mode`() = runTest {
        val (clock, _, engine) = makeEngine()
        val events = mutableListOf<CompletionEvent>()
        val job = launch { engine.completionEvents.collect { events.add(it) } }
        advanceUntilIdle()

        repeat(3) {
            engine.start()
            clock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick(notify = true)
            engine.skip() // skip the short break
        }
        // 4th focus block
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick(notify = true)

        advanceUntilIdle()
        job.cancel()
        val focusCompletions = events.filter { it.finishedMode == TimerMode.Focus }
        assertEquals(4, focusCompletions.size)
        assertEquals(TimerMode.LongBreak, focusCompletions.last().nextMode)
    }

    @Test
    fun `collecting all events over a full cycle yields one per completed block`() = runTest {
        val (clock, _, engine) = makeEngine()
        val events = mutableListOf<CompletionEvent>()
        val job = launch { engine.completionEvents.collect { events.add(it) } }
        advanceUntilIdle()

        // Complete 4 focus + 3 short breaks + 1 long break = 8 blocks total
        repeat(3) {
            engine.start()
            clock.advance(PomodoroConfig.FOCUS_DURATION)
            engine.tick(notify = true)

            engine.start()
            clock.advance(PomodoroConfig.SHORT_BREAK_DURATION)
            engine.tick(notify = true)
        }
        // 4th focus
        engine.start()
        clock.advance(PomodoroConfig.FOCUS_DURATION)
        engine.tick(notify = true)
        // Long break
        engine.start()
        clock.advance(PomodoroConfig.LONG_BREAK_DURATION)
        engine.tick(notify = true)

        advanceUntilIdle()
        job.cancel()
        assertEquals(8, events.size)
    }
}
