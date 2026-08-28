package com.emm.hello.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val lightScheme: ColorScheme = lightColorScheme(
    primary = ink,
    onPrimary = onInk,
    primaryContainer = surfaceRaised,
    onPrimaryContainer = ink,

    secondary = surfaceRaised,
    onSecondary = ink,
    secondaryContainer = surfaceRaised,
    onSecondaryContainer = ink,

    tertiary = successInk,
    onTertiary = onInk,
    tertiaryContainer = successContainer,
    onTertiaryContainer = successInk,

    error = destructiveInk,
    onError = onInk,
    errorContainer = destructiveContainer,
    onErrorContainer = destructiveInk,

    background = pageBackground,
    onBackground = ink,
    surface = surface,
    onSurface = ink,

    surfaceVariant = surfaceRaised,
    onSurfaceVariant = inkMuted,

    outline = outline,
    outlineVariant = hairline,

    inverseSurface = ink,
    inverseOnSurface = onInk,
    inversePrimary = onInk,

    scrim = ink,

    surfaceContainerLowest = surface,
    surfaceContainerLow = pageBackground,
    surfaceContainer = surfaceRaised,
    surfaceContainerHigh = surfaceRaised,
    surfaceContainerHighest = surfaceRaised,
    surfaceDim = surfaceRaised,
    surfaceBright = surface,
)

@Composable
fun HelloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightScheme,
        typography = appTypography,
        shapes = helloMaterialShapes,
    ) {
        ProvideHelloFoundations(
            semanticColors = helloSemanticColors(),
            content = content,
        )
    }
}
