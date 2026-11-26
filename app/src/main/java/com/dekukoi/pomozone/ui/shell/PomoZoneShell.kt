package com.dekukoi.pomozone.ui.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dekukoi.pomozone.ui.stats.StatsScreen
import com.dekukoi.pomozone.ui.theme.PomoZoneTheme
import com.dekukoi.pomozone.ui.timer.TimerScreen

@Composable
fun PomoZoneShell(
    state: ShellState,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            PomoBottomBar(
                selectedTab = state.selectedTab,
                onSelectTab = onSelectTab,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state.selectedTab) {
                AppTab.Timer -> TimerScreen()
                AppTab.Stats -> StatsScreen()
            }
        }
    }
}

@Composable
private fun PomoBottomBar(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .selectableGroup(),
            ) {
                BottomBarItem(
                    label = "Timer",
                    selected = selectedTab == AppTab.Timer,
                    onClick = { onSelectTab(AppTab.Timer) },
                    icon = { tint -> TimerTabIcon(tint = tint) },
                    modifier = Modifier.weight(1f),
                )
                BottomBarItem(
                    label = "Stats",
                    selected = selectedTab == AppTab.Stats,
                    onClick = { onSelectTab(AppTab.Stats) },
                    icon = { tint -> StatsTabIcon(tint = tint) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillBackground = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val iconTint = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val labelWeight = if (selected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(pillBackground),
            contentAlignment = Alignment.Center,
        ) {
            icon(iconTint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = labelWeight),
            color = labelColor,
        )
    }
}

@Composable
private fun TimerTabIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val stroke = Stroke(
            width = 2f * scaleX,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawCircle(
            color = tint,
            radius = 8f * scaleX,
            center = Offset(12f * scaleX, 13f * scaleY),
            style = stroke,
        )
        val path = Path().apply {
            moveTo(12f * scaleX, 9f * scaleY)
            lineTo(12f * scaleX, 13f * scaleY)
            lineTo(14.5f * scaleX, 15f * scaleY)
            moveTo(9.5f * scaleX, 2.5f * scaleY)
            lineTo(14.5f * scaleX, 2.5f * scaleY)
        }
        drawPath(path = path, color = tint, style = stroke)
    }
}

@Composable
private fun StatsTabIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val stroke = Stroke(
            width = 2f * scaleX,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val path = Path().apply {
            moveTo(5f * scaleX, 20f * scaleY)
            lineTo(5f * scaleX, 11f * scaleY)
            moveTo(12f * scaleX, 20f * scaleY)
            lineTo(12f * scaleX, 4f * scaleY)
            moveTo(19f * scaleX, 20f * scaleY)
            lineTo(19f * scaleX, 14f * scaleY)
        }
        drawPath(path = path, color = tint, style = stroke)
    }
}

@Preview(showBackground = true)
@Composable
private fun PomoZoneShellLightPreview() {
    PomoZoneTheme(darkTheme = false) {
        PomoZoneShell(
            state = ShellState(AppTab.Timer),
            onSelectTab = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PomoZoneShellDarkPreview() {
    PomoZoneTheme(darkTheme = true) {
        PomoZoneShell(
            state = ShellState(AppTab.Stats),
            onSelectTab = {},
        )
    }
}
