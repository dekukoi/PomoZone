package com.dekukoi.pomozone.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dekukoi.pomozone.domain.StatsModel
import com.dekukoi.pomozone.ui.theme.PomoZoneTheme

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    StatsScreen(
        state = state,
        modifier = modifier,
    )
}

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    state: StatsModel = StatsModel(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCards(state = state)
    }
}

@Composable
private fun SummaryCards(
    state: StatsModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                title = "Today",
                value = state.todayFormatted,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                title = "This week",
                value = state.thisWeekFormatted,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                title = "All time",
                value = state.allTimeFormatted,
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                title = "Streak",
                value = state.streakFormatted,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenLightPreview() {
    PomoZoneTheme(darkTheme = false) {
        StatsScreen(
            state = StatsModel(
                todayMinutes = 100L,
                thisWeekMinutes = 435L,
                allTimeMinutes = 2525L,
                streakDays = 5,
                totalSessions = 101,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenDarkPreview() {
    PomoZoneTheme(darkTheme = true) {
        StatsScreen(
            state = StatsModel(),
        )
    }
}
