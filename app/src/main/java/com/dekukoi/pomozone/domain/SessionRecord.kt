package com.dekukoi.pomozone.domain

import java.time.Instant

data class SessionRecord(
    val startInstant: Instant,
    val durationMinutes: Long,
)
