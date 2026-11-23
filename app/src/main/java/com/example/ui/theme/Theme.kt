package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StillroomDarkScheme = darkColorScheme(
    primary = StillroomAmber,
    onPrimary = StillroomObsidian,
    primaryContainer = StillroomUmber,
    onPrimaryContainer = StillroomLinen,
    secondary = StillroomSage,
    onSecondary = StillroomObsidian,
    secondaryContainer = StillroomCharcoal,
    onSecondaryContainer = StillroomLinen,
    tertiary = StillroomTerracotta,
    onTertiary = StillroomObsidian,
    background = StillroomObsidian,
    onBackground = StillroomLinen,
    surface = StillroomEspresso,
    onSurface = StillroomLinen,
    surfaceVariant = StillroomCharcoal,
    onSurfaceVariant = StillroomBone,
    outline = StillroomAsh.copy(alpha = 0.45f),
    error = StillroomCrimson,
    onError = StillroomLinen
)

val StillroomShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun StillroomTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StillroomDarkScheme,
        typography = StillroomTypography,
        shapes = StillroomShapes,
        content = content
    )
}
