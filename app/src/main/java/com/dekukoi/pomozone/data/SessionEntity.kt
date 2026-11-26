package com.dekukoi.pomozone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startEpochMilli: Long,
    val durationMinutes: Long,
)
