package com.dekukoi.pomozone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dekukoi.pomozone.ui.shell.AppTab
import com.dekukoi.pomozone.ui.shell.PomoZoneShell
import com.dekukoi.pomozone.ui.shell.ShellState
import com.dekukoi.pomozone.ui.theme.PomoZoneTheme
import com.dekukoi.pomozone.ui.timer.TimerScreen
import com.dekukoi.pomozone.ui.timer.TimerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PomoZoneApp).container
        check(container.clock.zone != null)
        setContent {
            var selectedTab by rememberSaveable { mutableStateOf(AppTab.Timer) }
            val shellState = ShellState(selectedTab = selectedTab)
            val timerViewModel: TimerViewModel = viewModel(
                factory = TimerViewModel.provideFactory(container.timerEngine),
            )
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                timerViewModel.refresh()
            }
            PomoZoneTheme {
                PomoZoneShell(
                    state = shellState,
                    onSelectTab = { tab ->
                        selectedTab = shellState.selectTab(tab).selectedTab
                    },
                    timerContent = {
                        TimerScreen(viewModel = timerViewModel)
                    },
                )
            }
        }
    }
}
