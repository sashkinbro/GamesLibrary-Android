package com.sbro.gameslibrary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,

    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,

    tertiary = OrangeTertiary,
    onTertiary = OrangeOnTertiary,
    tertiaryContainer = OrangeTertiaryContainer,
    onTertiaryContainer = OrangeOnTertiaryContainer,

    background = LightBackground,
    onBackground = Color(0xFF000000),
    surface = LightSurface,
    onSurface = Color(0xFF000000),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOutline,
    outline = LightOutline,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurplePrimaryContainer,
    onPrimaryContainer = PurpleOnPrimaryContainer,

    secondary = TealSecondary,
    onSecondary = TealOnSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealOnSecondaryContainer,

    tertiary = OrangeTertiary,
    onTertiary = OrangeOnTertiary,
    tertiaryContainer = OrangeTertiaryContainer,
    onTertiaryContainer = OrangeOnTertiaryContainer,

    background = DarkBackground,
    onBackground = Color(0xFFFFFFFF),
    surface = DarkSurface,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOutline,
    outline = DarkOutline,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun PSGamesTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
