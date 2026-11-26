package com.dekukoi.pomozone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dekukoi.pomozone.ui.shell.AppTab
import com.dekukoi.pomozone.ui.shell.PomoZoneShell
import com.dekukoi.pomozone.ui.shell.ShellState
import com.dekukoi.pomozone.ui.theme.PomoZoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PomoZoneApp).container
        check(container.clock.zone != null)
        setContent {
            var selectedTab by rememberSaveable { mutableStateOf(AppTab.Timer) }
            val shellState = ShellState(selectedTab = selectedTab)
            PomoZoneTheme {
                PomoZoneShell(
                    state = shellState,
                    onSelectTab = { tab ->
                        selectedTab = shellState.selectTab(tab).selectedTab
                    },
                )
            }
        }
    }
}
