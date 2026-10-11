package com.tidemark.habits.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette ("Beach Green")
val SandMist = Color(0xFFFAF7F0)
val SurfaceWhite = Color(0xFFFFFFFF)
val SeaMist = Color(0xFFEEF4F0)
val HairlineOutlineLight = Color(0xFFD5E3DC)
val TextDark = Color(0xFF1C2B27)
val TextMuted = Color(0xFF52635D)
val LagoonGreen = Color(0xFF1F7A6D)
val SeaGlass = Color(0xFFCFEBE1)
val OnSeaGlass = Color(0xFF0B3F37)
val CoralLight = Color(0xFFE2725B)
val ErrorLight = Color(0xFFB3402B)
val TrackLight = Color(0xFFDCEBE4)
val PartialLight = Color(0xFF9FD8C8)
val MissedSandLight = Color(0xFFF1E4D3)

// Dark Palette ("Night Sea")
val NightSeaBackground = Color(0xFF0E1A18)
val NightSeaSurface = Color(0xFF14231F)
val NightSeaVariant = Color(0xFF1C302B)
val HairlineOutlineDark = Color(0xFF35504A)
val TextLight = Color(0xFFE2EFEA)
val TextMutedDark = Color(0xFFA9BDB6)
val LagoonGreenDark = Color(0xFF6FD2BC)
val OnPrimaryDark = Color(0xFF00382E)
val PrimaryContainerDark = Color(0xFF174D43)
val OnPrimaryContainerDark = Color(0xFFBDEFE2)
val CoralDark = Color(0xFFF09A84)
val ErrorDark = Color(0xFFFFB4A3)
val TrackDark = Color(0xFF26403A)
val PartialDark = Color(0xFF2F7A6B)
val MissedNightDark = Color(0xFF2A332F)

/**
 * Extended color tokens for Tidemark accents and calendar states.
 */
@Immutable
data class TidemarkColors(
    val coral: Color,
    val track: Color,
    val calendarPartial: Color,
    val calendarMissed: Color
)

val LocalTidemarkColors = staticCompositionLocalOf {
    TidemarkColors(
        coral = CoralLight,
        track = TrackLight,
        calendarPartial = PartialLight,
        calendarMissed = MissedSandLight
    )
}
