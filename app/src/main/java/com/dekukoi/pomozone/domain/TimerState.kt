package com.dekukoi.pomozone.domain

import java.time.Duration

enum class TimerMode {
    Focus,
    ShortBreak,
    LongBreak,
}

enum class TimerStatus {
    Idle,
    Running,
    Paused,
}

data class TimerState(
    val mode: TimerMode = TimerMode.Focus,
    val status: TimerStatus = TimerStatus.Idle,
    val remainingTime: Duration = PomodoroConfig.FOCUS_DURATION,
    val totalDuration: Duration = PomodoroConfig.FOCUS_DURATION,
    val sessionInCycle: Int = 1,
)
