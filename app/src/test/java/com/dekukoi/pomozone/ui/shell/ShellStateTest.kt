package com.dekukoi.pomozone.ui.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellStateTest {
    @Test
    fun `initial tab is Timer`() {
        val state = ShellState()

        assertEquals(AppTab.Timer, state.selectedTab)
    }

    @Test
    fun `selecting Stats tab switches selected tab to Stats`() {
        val state = ShellState().selectTab(AppTab.Stats)

        assertEquals(AppTab.Stats, state.selectedTab)
    }

    @Test
    fun `selecting Timer tab after Stats switches back to Timer`() {
        val state = ShellState()
            .selectTab(AppTab.Stats)
            .selectTab(AppTab.Timer)

        assertEquals(AppTab.Timer, state.selectedTab)
    }
}
