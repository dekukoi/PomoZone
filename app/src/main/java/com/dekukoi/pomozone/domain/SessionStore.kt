package com.dekukoi.pomozone.domain

import kotlinx.coroutines.flow.Flow

interface SessionStore {
    suspend fun save(session: SessionRecord)
    fun sessions(): Flow<List<SessionRecord>>
}
