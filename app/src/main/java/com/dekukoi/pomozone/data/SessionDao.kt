package com.dekukoi.pomozone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY startEpochMilli ASC")
    fun getAllSessions(): Flow<List<SessionEntity>>
}
