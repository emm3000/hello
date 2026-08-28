package com.emm.hello.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.darkColorScheme as materialDarkColorScheme

private val darkScheme = darkColorScheme(
    semanticColors = instrumentSemanticColors(),
)

private fun darkColorScheme(
    @Suppress("UNUSED_PARAMETER") semanticColors: HelloSemanticColors,
) = materialDarkColorScheme(
    primary = instrumentPrimary,
    onPrimary = instrumentBg,
    primaryContainer = instrumentSurface2,
    onPrimaryContainer = instrumentOnBg,

    secondary = instrumentSurface2,
    onSecondary = instrumentOnBg,
    secondaryContainer = instrumentSurface2,
    onSecondaryContainer = instrumentOnBg,

    tertiary = instrumentGood,
    onTertiary = instrumentBg,
    tertiaryContainer = instrumentGoodSoft,
    onTertiaryContainer = instrumentGood,

    error = instrumentBad,
    onError = instrumentBg,
    errorContainer = instrumentBadSoft,
    onErrorContainer = instrumentBad,

    background = instrumentBg,
    onBackground = instrumentOnBg,
    surface = instrumentSurface,
    onSurface = instrumentOnBg,

    surfaceVariant = instrumentSurface2,
    onSurfaceVariant = instrumentMuted,

    outline = instrumentMuted,
    outlineVariant = instrumentDivider,

    inverseSurface = instrumentPrimary,
    inverseOnSurface = instrumentBg,
    inversePrimary = instrumentAccent,

    scrim = instrumentBg,

    surfaceContainerLowest = instrumentBg,
    surfaceContainerLow = instrumentSurface,
    surfaceContainer = instrumentSurface2,
    surfaceContainerHigh = instrumentElev,
    surfaceContainerHighest = instrumentElev,
    surfaceDim = instrumentBg,
    surfaceBright = instrumentSurface2,
)

@Composable
fun HelloTheme(
    // Dark-only product. isSystemInDarkTheme() is intentionally ignored; the
    // parameter is kept for call-site compatibility but has no effect.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = darkScheme,
        typography = appTypography,
        shapes = helloMaterialShapes,
    ) {
        ProvideHelloFoundations(
            semanticColors = instrumentSemanticColors(),
            content = content,
        )
    }
}
