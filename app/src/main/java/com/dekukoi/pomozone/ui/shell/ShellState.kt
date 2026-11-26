package com.dekukoi.pomozone.ui.shell

enum class AppTab {
    Timer,
    Stats,
}

data class ShellState(
    val selectedTab: AppTab = AppTab.Timer,
) {
    fun selectTab(tab: AppTab): ShellState = copy(selectedTab = tab)
}
