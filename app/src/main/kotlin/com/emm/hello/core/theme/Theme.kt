package com.emm.hello.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.darkColorScheme as materialDarkColorScheme
import androidx.compose.material3.lightColorScheme as materialLightColorScheme

/*
 * shadcn/ui Neutral → Material 3 color roles:
 *   primary             → shadcn --primary       (near-black / near-white)
 *   secondary           → shadcn --secondary     (light gray)
 *   tertiary            → shadcn success green
 *   error               → shadcn --destructive   (red)
 *   background/surface  → shadcn --background
 *   surfaceVariant      → shadcn --muted
 *   surfaceContainer    → shadcn --card
 *   outline             → shadcn --ring
 *   outlineVariant      → shadcn --border
 */
private val lightScheme = lightColorScheme(
    semanticColors = lightSemanticColors()
)

private fun lightColorScheme(
    semanticColors: HelloSemanticColors,
) = materialLightColorScheme(
    primary = shadcnPrimary,
    onPrimary = shadcnPrimaryFg,
    primaryContainer = shadcnSecondary,
    onPrimaryContainer = shadcnPrimary,

    secondary = shadcnMuted,
    onSecondary = shadcnForeground,
    secondaryContainer = shadcnSecondary,
    onSecondaryContainer = shadcnSecondaryFg,

    tertiary = semanticColors.success.accent,
    onTertiary = shadcnWhite,
    tertiaryContainer = semanticColors.success.container,
    onTertiaryContainer = semanticColors.success.content,

    error = semanticColors.destructive.accent,
    onError = shadcnDestructiveFg,
    errorContainer = semanticColors.destructive.container,
    onErrorContainer = semanticColors.destructive.content,

    background = shadcnBackground,
    onBackground = shadcnForeground,
    surface = shadcnBackground,
    onSurface = shadcnForeground,

    surfaceVariant = shadcnMuted,
    onSurfaceVariant = shadcnMutedFg,

    outline = shadcnRing,
    outlineVariant = shadcnBorder,

    inverseSurface = shadcnPrimary,
    inverseOnSurface = shadcnPrimaryFg,
    inversePrimary = shadcnDarkPrimary,

    scrim = shadcnBlack,

    surfaceContainerLowest = shadcnWhite,
    surfaceContainerLow = shadcnBackground,
    surfaceContainer = shadcnSecondary,
    surfaceContainerHigh = shadcnBorder,
    surfaceContainerHighest = shadcnInput,
    surfaceDim = shadcnBorder,
    surfaceBright = shadcnWhite,
)

private val darkScheme = darkColorScheme(
    semanticColors = darkSemanticColors()
)

private fun darkColorScheme(
    semanticColors: HelloSemanticColors,
) = materialDarkColorScheme(
    primary = shadcnDarkPrimary,
    onPrimary = shadcnDarkPrimaryFg,
    primaryContainer = shadcnDarkSecondary,
    onPrimaryContainer = shadcnDarkPrimary,

    secondary = shadcnDarkMuted,
    onSecondary = shadcnDarkForeground,
    secondaryContainer = shadcnDarkSecondary,
    onSecondaryContainer = shadcnDarkSecondaryFg,

    tertiary = semanticColors.success.accent,
    onTertiary = shadcnBlack,
    tertiaryContainer = semanticColors.success.container,
    onTertiaryContainer = semanticColors.success.content,

    error = semanticColors.destructive.accent,
    onError = shadcnDarkDestructiveFg,
    errorContainer = semanticColors.destructive.container,
    onErrorContainer = semanticColors.destructive.content,

    background = shadcnDarkBackground,
    onBackground = shadcnDarkForeground,
    surface = shadcnDarkBackground,
    onSurface = shadcnDarkForeground,

    surfaceVariant = shadcnDarkMuted,
    onSurfaceVariant = shadcnDarkMutedFg,

    outline = shadcnDarkRing,
    outlineVariant = shadcnDarkBorder,

    inverseSurface = shadcnDarkPrimary,
    inverseOnSurface = shadcnDarkPrimaryFg,
    inversePrimary = shadcnPrimary,

    scrim = shadcnBlack,

    surfaceContainerLowest = shadcnBlack,
    surfaceContainerLow = shadcnDarkBackground,
    surfaceContainer = shadcnDarkCard,
    surfaceContainerHigh = shadcnDarkSecondary,
    surfaceContainerHighest = shadcnDarkAccent,
    surfaceDim = shadcnDarkBackground,
    surfaceBright = shadcnDarkCard,
)

@Composable
fun HelloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val semanticColors = if (darkTheme) darkSemanticColors() else lightSemanticColors()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography,
        shapes = helloMaterialShapes,
    ) {
        ProvideHelloFoundations(
            semanticColors = semanticColors,
            content = content,
        )
    }
}
