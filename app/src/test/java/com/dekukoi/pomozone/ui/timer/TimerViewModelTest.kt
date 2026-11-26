package com.dekukoi.pomozone.ui.timer

import com.dekukoi.pomozone.data.InMemorySessionStore
import com.dekukoi.pomozone.domain.MutableFakeClock
import com.dekukoi.pomozone.domain.TimerEngine
import com.dekukoi.pomozone.domain.TimerMode
import com.dekukoi.pomozone.domain.TimerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val clock = MutableFakeClock(Instant.parse("2026-01-01T10:00:00Z"))
    private val sessionStore = InMemorySessionStore()
    private lateinit var engine: TimerEngine

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        engine = TimerEngine(clock = clock, sessionStore = sessionStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is focus mode with 25 minutes remaining`() {
        val viewModel = TimerViewModel(timerEngine = engine)
        val state = viewModel.state.value

        assertEquals(TimerMode.Focus, state.mode)
        assertEquals(TimerStatus.Idle, state.status)
        assertEquals(Duration.ofMinutes(25), state.remainingTime)
    }

    @Test
    fun `onStart transitions engine to running`() {
        val viewModel = TimerViewModel(timerEngine = engine)
        viewModel.onStart()

        assertEquals(TimerStatus.Running, viewModel.state.value.status)
    }

    @Test
    fun `refresh triggers immediate engine update when returning from background`() {
        val viewModel = TimerViewModel(timerEngine = engine)
        viewModel.onStart()
        assertEquals(TimerStatus.Running, viewModel.state.value.status)

        // Advance clock past 25 minutes while app was in background
        clock.advance(Duration.ofMinutes(30))

        // Immediate refresh when returning to app
        viewModel.refresh()
        testDispatcher.scheduler.runCurrent()

        // State is updated immediately to ShortBreak Idle
        assertEquals(TimerMode.ShortBreak, viewModel.state.value.mode)
        assertEquals(TimerStatus.Idle, viewModel.state.value.status)
    }

    @Test
    fun `timer state is preserved when a new view model receives the same engine after a configuration change`() {
        val viewModel1 = TimerViewModel(timerEngine = engine)
        viewModel1.onStart()
        clock.advance(Duration.ofMinutes(10))
        // Simulate a tick so remaining time is recorded in the engine
        viewModel1.refresh()
        testDispatcher.scheduler.runCurrent()

        val remainingBefore = viewModel1.state.value.remainingTime
        assertEquals(TimerStatus.Running, viewModel1.state.value.status)

        // Simulate config change: new ViewModel receives same engine (singleton in AppContainer)
        val viewModel2 = TimerViewModel(timerEngine = engine)

        // New ViewModel sees the same running state without any restart
        assertEquals(TimerStatus.Running, viewModel2.state.value.status)
        assertEquals(TimerMode.Focus, viewModel2.state.value.mode)
        assertEquals(remainingBefore, viewModel2.state.value.remainingTime)
    }
}
