package com.dekukoi.pomozone.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dekukoi.pomozone.domain.TimerEngine
import com.dekukoi.pomozone.domain.TimerState
import com.dekukoi.pomozone.notify.CompletionNotifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerViewModel(
    private val timerEngine: TimerEngine,
    private val completionNotifier: CompletionNotifier? = null,
) : ViewModel() {
    val state: StateFlow<TimerState> = timerEngine.state

    /**
     * Set by the host Activity to trigger the system permission dialog.
     * Called at most once (on the first Start press). After it is called,
     * the timer starts regardless of the permission outcome.
     */
    var onRequestNotificationPermission: (() -> Unit)? = null
    private var permissionRequested = false

    init {
        // Tick loop — passes notify=true so live completions emit a CompletionEvent.
        viewModelScope.launch {
            while (isActive) {
                timerEngine.tick(notify = true)
                delay(200L)
            }
        }

        // Collect completion events and forward them to the notifier.
        viewModelScope.launch {
            timerEngine.completionEvents.collect { event ->
                completionNotifier?.notify(event)
            }
        }
    }

    fun onStart() {
        if (!permissionRequested) {
            permissionRequested = true
            onRequestNotificationPermission?.invoke()
        }
        timerEngine.start()
    }

    fun onPause() {
        timerEngine.pause()
    }

    fun onResume() {
        timerEngine.resume()
    }

    fun onReset() {
        timerEngine.reset()
    }

    fun onSkip() {
        timerEngine.skip()
    }

    /**
     * Called on Activity resume to handle blocks that may have finished while
     * the process was backgrounded. Passes notify=false so no notification is
     * fired for a block that completed during suspension.
     */
    fun refresh() {
        viewModelScope.launch {
            timerEngine.tick(notify = false)
        }
    }

    companion object {
        fun provideFactory(
            timerEngine: TimerEngine,
            completionNotifier: CompletionNotifier? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimerViewModel(timerEngine, completionNotifier) as T
                }
            }
    }
}
