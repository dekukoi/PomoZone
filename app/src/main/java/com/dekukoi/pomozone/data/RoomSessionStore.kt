package com.dekukoi.pomozone.data

import com.dekukoi.pomozone.domain.SessionRecord
import com.dekukoi.pomozone.domain.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomSessionStore(
    private val sessionDao: SessionDao,
) : SessionStore {
    override suspend fun save(session: SessionRecord) {
        sessionDao.insert(
            SessionEntity(
                startEpochMilli = session.startInstant.toEpochMilli(),
                durationMinutes = session.durationMinutes,
            )
        )
    }

    override fun sessions(): Flow<List<SessionRecord>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { entity ->
                SessionRecord(
                    startInstant = Instant.ofEpochMilli(entity.startEpochMilli),
                    durationMinutes = entity.durationMinutes,
                )
            }
        }
    }
}
