package com.dekukoi.pomozone

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AppContainerTest {
    @Test
    fun `provides injected clock`() {
        val fixedInstant = Instant.parse("2026-11-26T10:00:00Z")
        val fakeClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        val container = AppContainer(clock = fakeClock)

        assertEquals(fixedInstant, container.clock.instant())
        assertEquals(ZoneOffset.UTC, container.clock.zone)
    }

    @Test
    fun `defaults to system default zone clock when no clock is injected`() {
        val container = AppContainer()

        assertEquals(ZoneId.systemDefault(), container.clock.zone)
    }
}
