package com.dekukoi.pomozone.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Duration
import java.time.Instant

class TimerEngine(
    private val clock: Clock,
    private val sessionStore: SessionStore,
) {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var endInstant: Instant? = null
    private var blockStartInstant: Instant? = null

    /**
     * Number of truly completed Focus sessions in the current cycle (0–4).
     * Only incremented when a Focus block runs to zero (never on skip or reset).
     * This drives the LongBreak trigger and the cycle reset.
     */
    private var completedInCycle: Int = 0

    fun start() {
        if (_state.value.status != TimerStatus.Running) {
            val now = clock.instant()
            if (blockStartInstant == null) {
                blockStartInstant = now
            }
            endInstant = now.plus(_state.value.remainingTime)
            _state.update { it.copy(status = TimerStatus.Running) }
        }
    }

    fun pause() {
        if (_state.value.status == TimerStatus.Running) {
            val now = clock.instant()
            val end = endInstant ?: return
            val remaining = Duration.between(now, end).coerceAtLeast(Duration.ZERO)
            endInstant = null
            _state.update { it.copy(status = TimerStatus.Paused, remainingTime = remaining) }
        }
    }

    fun resume() {
        if (_state.value.status == TimerStatus.Paused) {
            val now = clock.instant()
            endInstant = now.plus(_state.value.remainingTime)
            _state.update { it.copy(status = TimerStatus.Running) }
        }
    }

    fun reset() {
        endInstant = null
        blockStartInstant = null
        val currentMode = _state.value.mode
        val fullDuration = durationFor(currentMode)
        // completedInCycle and sessionInCycle are intentionally left unchanged by reset
        _state.update {
            it.copy(
                status = TimerStatus.Idle,
                remainingTime = fullDuration,
                totalDuration = fullDuration,
            )
        }
    }

    fun skip() {
        val currentMode = _state.value.mode
        endInstant = null
        blockStartInstant = null

        val (nextMode, nextSessionInCycle) = when (currentMode) {
            // Skipping a focus block: does NOT count toward the cycle.
            // Go to ShortBreak showing how many have been completed so far.
            TimerMode.Focus -> TimerMode.ShortBreak to completedInCycle

            // Skipping a break behaves exactly like completing it: advance to next focus.
            TimerMode.ShortBreak -> {
                val next = completedInCycle + 1
                TimerMode.Focus to next
            }
            TimerMode.LongBreak -> {
                // Reset cycle
                completedInCycle = 0
                TimerMode.Focus to 1
            }
        }

        val nextDuration = durationFor(nextMode)
        _state.value = TimerState(
            mode = nextMode,
            status = TimerStatus.Idle,
            remainingTime = nextDuration,
            totalDuration = nextDuration,
            sessionInCycle = nextSessionInCycle,
        )
    }

    suspend fun tick() {
        if (_state.value.status == TimerStatus.Running) {
            val now = clock.instant()
            val end = endInstant ?: return
            val remaining = Duration.between(now, end)
            if (remaining > Duration.ZERO) {
                _state.update { it.copy(remainingTime = remaining) }
            } else {
                completeCurrentBlock(now)
            }
        }
    }

    private suspend fun completeCurrentBlock(now: Instant) {
        val currentMode = _state.value.mode
        val start = blockStartInstant ?: now.minus(PomodoroConfig.FOCUS_DURATION)
        endInstant = null
        blockStartInstant = null

        val (nextMode, nextSessionInCycle) = when (currentMode) {
            TimerMode.Focus -> {
                // Save the session
                sessionStore.save(
                    SessionRecord(
                        startInstant = start,
                        durationMinutes = PomodoroConfig.FOCUS_DURATION.toMinutes(),
                    )
                )
                // Increment the completed count
                completedInCycle++
                if (completedInCycle >= PomodoroConfig.CYCLE_LENGTH) {
                    // 4th session done → long break
                    TimerMode.LongBreak to completedInCycle
                } else {
                    // 1st–3rd done → short break
                    TimerMode.ShortBreak to completedInCycle
                }
            }
            TimerMode.ShortBreak -> {
                // Break over → next focus block
                val next = completedInCycle + 1
                TimerMode.Focus to next
            }
            TimerMode.LongBreak -> {
                // Long break over → reset cycle
                completedInCycle = 0
                TimerMode.Focus to 1
            }
        }

        val nextDuration = durationFor(nextMode)
        _state.value = TimerState(
            mode = nextMode,
            status = TimerStatus.Idle,
            remainingTime = nextDuration,
            totalDuration = nextDuration,
            sessionInCycle = nextSessionInCycle,
        )
    }

    private fun durationFor(mode: TimerMode): Duration = when (mode) {
        TimerMode.Focus -> PomodoroConfig.FOCUS_DURATION
        TimerMode.ShortBreak -> PomodoroConfig.SHORT_BREAK_DURATION
        TimerMode.LongBreak -> PomodoroConfig.LONG_BREAK_DURATION
    }
}
