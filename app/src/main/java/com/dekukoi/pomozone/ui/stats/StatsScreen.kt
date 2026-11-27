package com.dekukoi.pomozone.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dekukoi.pomozone.domain.DayBarEntry
import com.dekukoi.pomozone.domain.HeatmapCell
import com.dekukoi.pomozone.domain.HeatmapWeek
import com.dekukoi.pomozone.domain.StatsModel
import com.dekukoi.pomozone.ui.theme.PomoTheme
import com.dekukoi.pomozone.ui.theme.PomoZoneTheme
import java.time.DayOfWeek
import java.time.LocalDate

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
        GoalPanel(state = state)
        WeeklyBarsPanel(bars = state.weeklyBars)
        HeatmapPanel(weeks = state.heatmapWeeks)
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

@Composable
private fun GoalPanel(
    state: StatsModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GoalRing(
                fraction = state.goalFraction,
                modifier = Modifier.size(56.dp),
            )
            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.goalLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "sessions today",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GoalRing(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val progressColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )
        val arcSize = Size(diameter, diameter)

        // Track in line color
        drawArc(
            color = outlineColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )

        // Progress arc in focus color (starts at top, fills clockwise)
        if (fraction > 0f) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun WeeklyBarsPanel(
    bars: List<DayBarEntry>,
    modifier: Modifier = Modifier,
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    val maxMinutes = bars.maxOfOrNull { (_, _, _, minutes, _) -> minutes }?.coerceAtLeast(1L) ?: 1L
    val todayHighlightColor = PomoTheme.colors.heat[4]
    val barColor = MaterialTheme.colorScheme.primary
    val muteColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = "LAST 7 DAYS (MINUTES)",
                style = MaterialTheme.typography.labelMedium,
                color = muteColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { (_, _, label, minutes, isToday) ->
                    val color = if (isToday) todayHighlightColor else barColor
                    val targetHeightRatio = if (maxMinutes > 0L) minutes.toFloat() / maxMinutes else 0f
                    val animatedHeightRatio = targetHeightRatio * animProgress.value

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(78.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (animatedHeightRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animatedHeightRatio)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 5.dp,
                                                topEnd = 5.dp,
                                                bottomStart = 2.dp,
                                                bottomEnd = 2.dp,
                                            ),
                                        )
                                        .background(color),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal),
                            color = muteColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapPanel(
    weeks: List<HeatmapWeek>,
    modifier: Modifier = Modifier,
) {
    val muteColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = "LAST 12 WEEKS",
                style = MaterialTheme.typography.labelMedium,
                color = muteColor,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                weeks.forEach { (days) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        days.forEach { (_, _, _, level, isFuture) ->
                            val cellModifier = Modifier
                                .fillMaxWidth()
                                .height(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                            if (isFuture) {
                                Box(modifier = cellModifier)
                            } else {
                                Box(
                                    modifier = cellModifier.background(PomoTheme.colors.heat[level]),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Less",
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal),
                    color = muteColor,
                )
                for (level in 0..4) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PomoTheme.colors.heat[level]),
                    )
                }
                Text(
                    text = "More",
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Normal),
                    color = muteColor,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenLightPreview() {
    val baseDate = LocalDate.parse("2026-06-15")
    val sampleBars = listOf(
        DayBarEntry(baseDate.minusDays(6), DayOfWeek.TUESDAY, "T", 50L, isToday = false),
        DayBarEntry(baseDate.minusDays(5), DayOfWeek.WEDNESDAY, "W", 75L, isToday = false),
        DayBarEntry(baseDate.minusDays(4), DayOfWeek.THURSDAY, "T", 25L, isToday = false),
        DayBarEntry(baseDate.minusDays(3), DayOfWeek.FRIDAY, "F", 100L, isToday = false),
        DayBarEntry(baseDate.minusDays(2), DayOfWeek.SATURDAY, "S", 0L, isToday = false),
        DayBarEntry(baseDate.minusDays(1), DayOfWeek.SUNDAY, "S", 50L, isToday = false),
        DayBarEntry(baseDate, DayOfWeek.MONDAY, "M", 75L, isToday = true),
    )
    val sampleWeeks = (0..11).map { weekIndex ->
        val weekDate = baseDate.minusWeeks((11 - weekIndex).toLong())
        HeatmapWeek(
            days = (0..6).map { dayIndex ->
                val date = weekDate.plusDays(dayIndex.toLong())
                val isFuture = (weekIndex == 11) && (dayIndex > 0)
                HeatmapCell(
                    date = date,
                    dayOfWeek = date.dayOfWeek,
                    minutes = if (isFuture) 0L else ((((weekIndex * 7) + dayIndex) * 15L) % 125L),
                    level = if (isFuture) 0 else ((weekIndex + dayIndex) % 5),
                    isFuture = isFuture,
                )
            },
        )
    }
    PomoZoneTheme(darkTheme = false) {
        StatsScreen(
            state = StatsModel(
                todayMinutes = 75L,
                thisWeekMinutes = 375L,
                allTimeMinutes = 2525L,
                streakDays = 5,
                totalSessions = 101,
                todaySessions = 3,
                weeklyBars = sampleBars,
                heatmapWeeks = sampleWeeks,
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
