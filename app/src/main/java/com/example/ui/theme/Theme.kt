package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val MobileLabsColorScheme = darkColorScheme(
    primary = MintAccent,
    onPrimary = StudioVoid,
    primaryContainer = MintAccentDim,
    onPrimaryContainer = MintAccentBright,
    secondary = MintAccentBright,
    onSecondary = StudioVoid,
    secondaryContainer = StudioElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = LiveCrimsonBright,
    onTertiary = TextPrimary,
    background = StudioVoid,
    onBackground = TextPrimary,
    surface = StudioDock,
    onSurface = TextPrimary,
    surfaceVariant = StudioHeader,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    outlineVariant = StudioBorderBright,
    error = LiveCrimsonBright,
    onError = TextPrimary
)

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun MobileLabsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MobileLabsColorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
