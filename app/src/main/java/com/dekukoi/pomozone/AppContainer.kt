package com.dekukoi.pomozone

import android.content.Context
import com.dekukoi.pomozone.data.AppDatabase
import com.dekukoi.pomozone.data.InMemorySessionStore
import com.dekukoi.pomozone.data.RoomSessionStore
import com.dekukoi.pomozone.domain.SessionStore
import com.dekukoi.pomozone.domain.TimerEngine
import com.dekukoi.pomozone.notify.CompletionNotifier
import java.time.Clock

class AppContainer(
    val clock: Clock = Clock.systemDefaultZone(),
    val sessionStore: SessionStore = InMemorySessionStore(),
    val timerEngine: TimerEngine = TimerEngine(clock = clock, sessionStore = sessionStore),
    val completionNotifier: CompletionNotifier? = null,
) {
    companion object {
        fun create(context: Context, clock: Clock = Clock.systemDefaultZone()): AppContainer {
            val database = AppDatabase.build(context)
            val sessionStore = RoomSessionStore(database.sessionDao())
            val timerEngine = TimerEngine(clock = clock, sessionStore = sessionStore)
            val completionNotifier = CompletionNotifier(context)
            return AppContainer(
                clock = clock,
                sessionStore = sessionStore,
                timerEngine = timerEngine,
                completionNotifier = completionNotifier,
            )
        }
    }
}
