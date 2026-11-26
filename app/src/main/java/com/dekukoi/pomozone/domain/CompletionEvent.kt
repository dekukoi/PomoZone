package com.dekukoi.pomozone.domain

/**
 * Emitted by [TimerEngine] exactly once per block that completes while the app
 * is actively observing (i.e., the tick loop is running).
 *
 * [finishedMode] is the mode that just ended.
 * [nextMode]     is the mode that the engine has moved to.
 */
data class CompletionEvent(
    val finishedMode: TimerMode,
    val nextMode: TimerMode,
)
