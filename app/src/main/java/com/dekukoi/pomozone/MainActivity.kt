package com.dekukoi.pomozone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.dekukoi.pomozone.ui.stats.StatsScreen
import com.dekukoi.pomozone.ui.stats.StatsViewModel
import com.dekukoi.pomozone.ui.theme.PomoZoneTheme
import com.dekukoi.pomozone.ui.timer.TimerScreen
import com.dekukoi.pomozone.ui.timer.TimerViewModel

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Permission outcome is intentionally ignored — the timer works either way.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PomoZoneApp).container
        check(container.clock.zone != null)
        setContent {
            var selectedTab by rememberSaveable { mutableStateOf(AppTab.Timer) }
            val shellState = ShellState(selectedTab = selectedTab)
            val timerViewModel: TimerViewModel = viewModel(
                factory = TimerViewModel.provideFactory(
                    timerEngine = container.timerEngine,
                    completionNotifier = container.completionNotifier,
                ),
            )
            val statsViewModel: StatsViewModel = viewModel(
                factory = StatsViewModel.provideFactory(
                    sessionStore = container.sessionStore,
                    clock = container.clock,
                ),
            )

            // Wire permission request: called at most once, on first Start press.
            timerViewModel.onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val alreadyGranted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                    if (!alreadyGranted) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

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
                    statsContent = {
                        StatsScreen(viewModel = statsViewModel)
                    },
                )
            }
        }
    }
}
