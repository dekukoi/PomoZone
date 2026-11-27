package com.dekukoi.pomozone.ui.stats

import com.dekukoi.pomozone.data.InMemorySessionStore
import com.dekukoi.pomozone.domain.MutableFakeClock
import com.dekukoi.pomozone.domain.SessionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-06-15T10:00:00Z")
    private val clock = MutableFakeClock(now, ZoneOffset.UTC)
    private val sessionStore = InMemorySessionStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows zeros when there are no sessions`() = runTest {
        val viewModel = StatsViewModel(sessionStore = sessionStore, clock = clock, zoneId = ZoneOffset.UTC)
        val job = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0L, state.todayMinutes)
        assertEquals(0L, state.thisWeekMinutes)
        assertEquals(0L, state.allTimeMinutes)
        assertEquals(0, state.streakDays)
        assertEquals(0, state.totalSessions)
        assertEquals("0", state.todayFormatted)
        assertEquals("0 days", state.streakFormatted)

        job.cancel()
    }

    @Test
    fun `cards update live when a session is saved with no refresh action`() = runTest {
        val viewModel = StatsViewModel(sessionStore = sessionStore, clock = clock, zoneId = ZoneOffset.UTC)
        val job = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(0L, viewModel.state.value.todayMinutes)
        assertEquals(0, viewModel.state.value.totalSessions)

        // Save a session for today (25 min)
        sessionStore.save(SessionRecord(startInstant = Instant.parse("2026-06-15T09:00:00Z"), durationMinutes = 25L))
        advanceUntilIdle()

        assertEquals(25L, viewModel.state.value.todayMinutes)
        assertEquals(25L, viewModel.state.value.thisWeekMinutes)
        assertEquals(25L, viewModel.state.value.allTimeMinutes)
        assertEquals(1, viewModel.state.value.streakDays)
        assertEquals(1, viewModel.state.value.totalSessions)
        assertEquals("25m", viewModel.state.value.todayFormatted)
        assertEquals("1 day", viewModel.state.value.streakFormatted)

        // Save another session for yesterday (25 min)
        sessionStore.save(SessionRecord(startInstant = Instant.parse("2026-06-14T09:00:00Z"), durationMinutes = 25L))
        advanceUntilIdle()

        assertEquals(25L, viewModel.state.value.todayMinutes)
        assertEquals(50L, viewModel.state.value.thisWeekMinutes)
        assertEquals(50L, viewModel.state.value.allTimeMinutes)
        assertEquals(2, viewModel.state.value.streakDays)
        assertEquals(2, viewModel.state.value.totalSessions)
        assertEquals("2 days", viewModel.state.value.streakFormatted)

        job.cancel()
    }
}
