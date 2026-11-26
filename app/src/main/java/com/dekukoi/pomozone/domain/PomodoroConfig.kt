package com.dekukoi.pomozone.domain

import java.time.Duration

object PomodoroConfig {
    val FOCUS_DURATION: Duration = Duration.ofMinutes(25)
    val SHORT_BREAK_DURATION: Duration = Duration.ofMinutes(5)
    val LONG_BREAK_DURATION: Duration = Duration.ofMinutes(15)
    const val DAILY_GOAL: Int = 4

    const val HEATMAP_LEVEL_0_MAX: Long = 0L
    const val HEATMAP_LEVEL_1_MAX: Long = 25L
    const val HEATMAP_LEVEL_2_MAX: Long = 50L
    const val HEATMAP_LEVEL_3_MAX: Long = 100L
}
