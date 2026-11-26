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
        val fullDuration = when (currentMode) {
            TimerMode.Focus -> PomodoroConfig.FOCUS_DURATION
            TimerMode.ShortBreak -> PomodoroConfig.SHORT_BREAK_DURATION
            TimerMode.LongBreak -> PomodoroConfig.LONG_BREAK_DURATION
        }
        _state.update {
            it.copy(
                status = TimerStatus.Idle,
                remainingTime = fullDuration,
                totalDuration = fullDuration,
            )
        }
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

        if (currentMode == TimerMode.Focus) {
            sessionStore.save(
                SessionRecord(
                    startInstant = start,
                    durationMinutes = PomodoroConfig.FOCUS_DURATION.toMinutes(),
                )
            )
        }

        // Until ticket 04 adds breaks, simply return to an idle Focus block
        _state.value = TimerState(
            mode = TimerMode.Focus,
            status = TimerStatus.Idle,
            remainingTime = PomodoroConfig.FOCUS_DURATION,
            totalDuration = PomodoroConfig.FOCUS_DURATION,
            sessionInCycle = _state.value.sessionInCycle,
        )
    }
}
