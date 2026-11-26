package com.dekukoi.pomozone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App colors that Material's ColorScheme has no slot for. */
@Immutable
data class PomoColors(
    val focus: Color,
    val shortBreak: Color,
    val longBreak: Color,
    /** Five heatmap levels, index 0 (no focus) to 4 (more than 100 minutes). */
    val heat: List<Color>,
)

private val LightPomoColors = PomoColors(
    focus = FocusRed,
    shortBreak = BreakGreen,
    longBreak = LongBreakBlue,
    heat = listOf(LightHeat0, LightHeat1, LightHeat2, LightHeat3, LightHeat4),
)

private val DarkPomoColors = PomoColors(
    focus = FocusRed,
    shortBreak = BreakGreen,
    longBreak = LongBreakBlue,
    heat = listOf(DarkHeat0, DarkHeat1, DarkHeat2, DarkHeat3, DarkHeat4),
)

private val LocalPomoColors = staticCompositionLocalOf { LightPomoColors }

object PomoTheme {
    val colors: PomoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPomoColors.current
}

private val LightScheme = lightColorScheme(
    primary = FocusRed,
    onPrimary = Color.White,
    primaryContainer = LightHeat1,
    onPrimaryContainer = LightHeat4,
    secondary = BreakGreen,
    onSecondary = Color.White,
    tertiary = LongBreakBlue,
    onTertiary = Color.White,
    background = LightPanel,
    onBackground = LightInk,
    surface = LightPanel,
    onSurface = LightInk,
    surfaceVariant = LightCanvas,
    onSurfaceVariant = LightMute,
    outline = LightLine,
    outlineVariant = LightLine,
)

private val DarkScheme = darkColorScheme(
    primary = FocusRed,
    onPrimary = Color.White,
    primaryContainer = DarkHeat1,
    onPrimaryContainer = DarkHeat4,
    secondary = BreakGreen,
    onSecondary = Color.White,
    tertiary = LongBreakBlue,
    onTertiary = Color.White,
    background = DarkPanel,
    onBackground = DarkInk,
    surface = DarkPanel,
    onSurface = DarkInk,
    surfaceVariant = DarkCanvas,
    onSurfaceVariant = DarkMute,
    outline = DarkLine,
    outlineVariant = DarkLine,
)

@Composable
fun PomoZoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPomoColors provides if (darkTheme) DarkPomoColors else LightPomoColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography,
            content = content,
        )
    }
}
