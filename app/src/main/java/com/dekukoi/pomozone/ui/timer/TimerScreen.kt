package com.dekukoi.pomozone.ui.timer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dekukoi.pomozone.domain.TimerMode
import com.dekukoi.pomozone.domain.TimerState
import com.dekukoi.pomozone.domain.TimerStatus
import com.dekukoi.pomozone.ui.theme.PomoTheme
import com.dekukoi.pomozone.ui.theme.PomoZoneTheme
import java.util.Locale

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    TimerScreen(
        state = state,
        onStartClick = viewModel::onStart,
        modifier = modifier,
    )
}

@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    state: TimerState = TimerState(),
    onStartClick: () -> Unit = {},
    onResetClick: () -> Unit = {},
    onSkipClick: () -> Unit = {},
) {
    val modeColor = when (state.mode) {
        TimerMode.Focus -> MaterialTheme.colorScheme.primary
        TimerMode.ShortBreak -> PomoTheme.colors.shortBreak
        TimerMode.LongBreak -> PomoTheme.colors.longBreak
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ModeChipsRow(
            currentMode = state.mode,
            focusColor = MaterialTheme.colorScheme.primary,
            shortBreakColor = PomoTheme.colors.shortBreak,
            longBreakColor = PomoTheme.colors.longBreak,
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
        )

        ProgressRing(
            state = state,
            modeColor = modeColor,
            modifier = Modifier.padding(vertical = 10.dp),
        )

        SessionDotsRow(
            currentSession = state.sessionInCycle,
            focusColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        TimerControlsRow(
            status = state.status,
            modeColor = modeColor,
            onStartClick = onStartClick,
            onResetClick = onResetClick,
            onSkipClick = onSkipClick,
            modifier = Modifier.padding(bottom = 14.dp),
        )
    }
}

@Composable
private fun ModeChipsRow(
    currentMode: TimerMode,
    focusColor: Color,
    shortBreakColor: Color,
    longBreakColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeChip(
            label = "Focus",
            selected = currentMode == TimerMode.Focus,
            selectedColor = focusColor,
        )
        ModeChip(
            label = "Short break",
            selected = currentMode == TimerMode.ShortBreak,
            selectedColor = shortBreakColor,
        )
        ModeChip(
            label = "Long break",
            selected = currentMode == TimerMode.LongBreak,
            selectedColor = longBreakColor,
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
) {
    val chipShape = RoundedCornerShape(999.dp)
    val backgroundModifier = if (selected) {
        Modifier.background(selectedColor, chipShape)
    } else {
        Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), chipShape)
    }

    Box(
        modifier = modifier
            .then(backgroundModifier)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProgressRing(
    state: TimerState,
    modeColor: Color,
    modifier: Modifier = Modifier,
) {
    val targetProgress = if (state.totalDuration.toMillis() > 0) {
        (state.remainingTime.toMillis().toFloat() / state.totalDuration.toMillis()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "ringProgress",
    )

    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier.size(216.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 12.dp.toPx()
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            val topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

            // Track
            drawArc(
                color = outlineColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            // Progress: drains clockwise (starts at top, sweep decreases)
            if (animatedProgress > 0.001f) {
                drawArc(
                    color = modeColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val totalSeconds = state.remainingTime.seconds.coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val timeString = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

            Text(
                text = timeString,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            val sessionLabel = if (state.mode == TimerMode.Focus) {
                "Session ${state.sessionInCycle} of 4"
            } else {
                "${state.sessionInCycle - 1} of 4 done"
            }
            Text(
                text = sessionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionDotsRow(
    currentSession: Int,
    focusColor: Color,
    modifier: Modifier = Modifier,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..4) {
            when {
                i < currentSession -> {
                    // Completed: filled in focus color
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(focusColor, CircleShape),
                    )
                }
                i == currentSession -> {
                    // Current block: outline of 2 in focus color
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .border(BorderStroke(2.dp, focusColor), CircleShape),
                    )
                }
                else -> {
                    // Remaining: line color
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(outlineColor, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerControlsRow(
    status: TimerStatus,
    modeColor: Color,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val muteColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Reset button
        Box(
            modifier = Modifier
                .size(46.dp)
                .border(BorderStroke(1.dp, outlineColor), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onResetClick)
                .semantics { contentDescription = "Reset" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "↺",
                color = muteColor,
                fontSize = 18.sp,
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        // Main button (Start / Pause / Resume)
        val mainDescription = when (status) {
            TimerStatus.Idle -> "Start"
            TimerStatus.Running -> "Pause"
            TimerStatus.Paused -> "Resume"
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(modeColor, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onStartClick)
                .semantics { contentDescription = mainDescription },
            contentAlignment = Alignment.Center,
        ) {
            if (status == TimerStatus.Running) {
                // Pause icon
                Canvas(modifier = Modifier.size(26.dp)) {
                    val barWidth = 6.dp.toPx()
                    val barHeight = 20.dp.toPx()
                    val topY = (size.height - barHeight) / 2f
                    val gap = 6.dp.toPx()
                    val totalWidth = barWidth * 2 + gap
                    val startX = (size.width - totalWidth) / 2f

                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(startX, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(startX + barWidth + gap, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            } else {
                // Play / Start icon (play triangle)
                Canvas(modifier = Modifier.size(26.dp)) {
                    val triangleWidth = 20.dp.toPx()
                    val triangleHeight = 22.dp.toPx()
                    val leftX = (size.width - triangleWidth) / 2f + 2.dp.toPx()
                    val topY = (size.height - triangleHeight) / 2f

                    val path = Path().apply {
                        moveTo(leftX, topY)
                        lineTo(leftX + triangleWidth, topY + triangleHeight / 2f)
                        lineTo(leftX, topY + triangleHeight)
                        close()
                    }
                    drawPath(path = path, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        // Skip button
        Box(
            modifier = Modifier
                .size(46.dp)
                .border(BorderStroke(1.dp, outlineColor), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onSkipClick)
                .semantics { contentDescription = "Skip" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⏭",
                color = muteColor,
                fontSize = 16.sp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenPreview() {
    PomoZoneTheme {
        TimerScreen()
    }
}
