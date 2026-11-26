package com.dekukoi.pomozone.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dekukoi.pomozone.domain.TimerEngine
import com.dekukoi.pomozone.domain.TimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerViewModel(
    private val timerEngine: TimerEngine,
) : ViewModel() {
    val state: StateFlow<TimerState> = timerEngine.state

    init {
        viewModelScope.launch {
            while (isActive) {
                timerEngine.tick()
                delay(200L)
            }
        }
    }

    fun onStart() {
        timerEngine.start()
    }

    companion object {
        fun provideFactory(timerEngine: TimerEngine): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimerViewModel(timerEngine) as T
                }
            }
    }
}
