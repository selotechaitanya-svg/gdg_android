package com.tidemark.habits.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.tidemark.habits.data.settings.AppTheme

private val LightColorScheme = lightColorScheme(
    primary = LagoonGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = SeaGlass,
    onPrimaryContainer = OnSeaGlass,
    background = SandMist,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = SeaMist,
    onSurfaceVariant = TextMuted,
    outline = HairlineOutlineLight,
    error = ErrorLight
)

private val DarkColorScheme = darkColorScheme(
    primary = LagoonGreenDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    background = NightSeaBackground,
    onBackground = TextLight,
    surface = NightSeaSurface,
    onSurface = TextLight,
    surfaceVariant = NightSeaVariant,
    onSurfaceVariant = TextMutedDark,
    outline = HairlineOutlineDark,
    error = ErrorDark
)

private val LightTidemarkColors = TidemarkColors(
    coral = CoralLight,
    track = TrackLight,
    calendarPartial = PartialLight,
    calendarMissed = MissedSandLight
)

private val DarkTidemarkColors = TidemarkColors(
    coral = CoralDark,
    track = TrackDark,
    calendarPartial = PartialDark,
    calendarMissed = MissedNightDark
)

/**
 * Beach Green design system entry point.
 * Enforces stable brand identity by keeping Material You dynamic color disabled.
 */
@Composable
fun TidemarkTheme(
    themeSetting: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeSetting) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val tidemarkColors = if (isDark) DarkTidemarkColors else LightTidemarkColors

    CompositionLocalProvider(LocalTidemarkColors provides tidemarkColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
