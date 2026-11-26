package com.dekukoi.pomozone.domain

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableFakeClock(
    private var currentInstant: Instant = Instant.parse("2026-01-01T10:00:00Z"),
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableFakeClock(currentInstant, zone)

    override fun instant(): Instant = currentInstant

    fun advance(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }

    fun setInstant(newInstant: Instant) {
        currentInstant = newInstant
    }
}
