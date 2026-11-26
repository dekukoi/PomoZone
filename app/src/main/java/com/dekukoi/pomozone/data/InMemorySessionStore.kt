package com.dekukoi.pomozone.data

import com.dekukoi.pomozone.domain.SessionRecord
import com.dekukoi.pomozone.domain.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemorySessionStore(
    initialSessions: List<SessionRecord> = emptyList(),
) : SessionStore {
    private val _sessions = MutableStateFlow(initialSessions)

    override suspend fun save(session: SessionRecord) {
        _sessions.update { it + session }
    }

    override fun sessions(): Flow<List<SessionRecord>> = _sessions.asStateFlow()
}
