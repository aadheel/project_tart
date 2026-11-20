package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TaktDarkColorScheme = darkColorScheme(
    primary = TaktVermilion,
    onPrimary = Color.White,
    primaryContainer = TaktVermilionContainerDark,
    onPrimaryContainer = TaktVermilion,
    secondary = TaktDarkOnBackground,
    onSecondary = TaktDarkBackground,
    secondaryContainer = TaktDarkSurfaceElevated,
    onSecondaryContainer = TaktDarkOnBackground,
    background = TaktDarkBackground,
    onBackground = TaktDarkOnBackground,
    surface = TaktDarkSurface,
    onSurface = TaktDarkOnBackground,
    surfaceVariant = TaktDarkSurfaceElevated,
    onSurfaceVariant = TaktDarkOnSurfaceVariant,
    outline = TaktDarkOutline,
    outlineVariant = TaktDarkOutline,
    error = TaktError,
    onError = Color.White
)

private val TaktLightColorScheme = lightColorScheme(
    primary = TaktLightVermilion,
    onPrimary = Color.White,
    primaryContainer = TaktVermilionContainerLight,
    onPrimaryContainer = TaktLightVermilion,
    secondary = TaktLightOnBackground,
    onSecondary = TaktLightBackground,
    secondaryContainer = TaktLightSurfaceElevated,
    onSecondaryContainer = TaktLightOnBackground,
    background = TaktLightBackground,
    onBackground = TaktLightOnBackground,
    surface = TaktLightSurface,
    onSurface = TaktLightOnBackground,
    surfaceVariant = TaktLightSurfaceElevated,
    onSurfaceVariant = TaktLightOnSurfaceVariant,
    outline = TaktLightOutline,
    outlineVariant = TaktLightOutline,
    error = TaktError,
    onError = Color.White
)

val TaktShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TaktTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TaktDarkColorScheme else TaktLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TaktTypography,
        shapes = TaktShapes,
        content = content
    )
}
