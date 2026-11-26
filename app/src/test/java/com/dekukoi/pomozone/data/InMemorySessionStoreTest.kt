package com.dekukoi.pomozone.data

import com.dekukoi.pomozone.domain.SessionRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class InMemorySessionStoreTest {
    @Test
    fun `saving session emits updated session list`() = runTest {
        val store = InMemorySessionStore()
        assertEquals(emptyList<SessionRecord>(), store.sessions().first())

        val session = SessionRecord(
            startInstant = Instant.parse("2026-01-01T10:00:00Z"),
            durationMinutes = 25,
        )
        store.save(session)

        val updated = store.sessions().first()
        assertEquals(1, updated.size)
        assertEquals(session, updated[0])
    }
}
