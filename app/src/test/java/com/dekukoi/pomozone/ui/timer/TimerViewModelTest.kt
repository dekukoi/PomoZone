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
}
