package com.dekukoi.pomozone.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dekukoi.pomozone.domain.SessionStore
import com.dekukoi.pomozone.domain.StatsModel
import com.dekukoi.pomozone.domain.StatsProjection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.ZoneId

class StatsViewModel(
    sessionStore: SessionStore,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = clock.zone,
) : ViewModel() {

    val state: StateFlow<StatsModel> = sessionStore.sessions()
        .map { sessions ->
            StatsProjection.project(
                sessions = sessions,
                currentInstant = clock.instant(),
                zoneId = zoneId,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatsProjection.project(
                sessions = emptyList(),
                currentInstant = clock.instant(),
                zoneId = zoneId,
            ),
        )

    companion object {
        fun provideFactory(
            sessionStore: SessionStore,
            clock: Clock = Clock.systemDefaultZone(),
            zoneId: ZoneId = clock.zone,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatsViewModel(sessionStore, clock, zoneId) as T
                }
            }
    }
}
