package com.emm.hello.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.darkColorScheme as materialDarkColorScheme
import androidx.compose.material3.lightColorScheme as materialLightColorScheme

/*
 * shadcn/ui Neutral → Material 3 color roles (light scheme kept for Phase 5):
 *   primary             → shadcn --primary       (near-black / near-white)
 *   secondary           → shadcn --secondary     (light gray)
 *   tertiary            → shadcn success green
 *   error               → shadcn --destructive   (red)
 *   background/surface  → shadcn --background
 *   surfaceVariant      → shadcn --muted
 *   surfaceContainer    → shadcn --card
 *   outline             → shadcn --ring
 *   outlineVariant      → shadcn --border
 *
 * Phase 0: dark mode is forced. The light scheme function is retained for Phase 5
 * but the private val is intentionally not instantiated to avoid the unused-property
 * warning while the light path is deferred.
 */

@Suppress("UnusedPrivateMember")
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
    semanticColors = darkSemanticColors(),
)

private fun darkColorScheme(
    @Suppress("UNUSED_PARAMETER") semanticColors: HelloSemanticColors,
) = materialDarkColorScheme(
    // ── Ember Dark tokens wired to Material 3 roles ──────────────────────────
    primary = emberPrimary,
    onPrimary = emberBg,
    primaryContainer = emberSurface2,
    onPrimaryContainer = emberOnBg,

    secondary = emberSurface2,
    onSecondary = emberOnBg,
    secondaryContainer = emberSurface2,
    onSecondaryContainer = emberOnBg,

    tertiary = emberGood,
    onTertiary = emberBg,
    tertiaryContainer = emberGoodSoft,
    onTertiaryContainer = emberGood,

    error = emberBad,
    onError = emberBg,
    errorContainer = emberBadSoft,
    onErrorContainer = emberBad,

    background = emberBg,
    onBackground = emberOnBg,
    surface = emberSurface,
    onSurface = emberOnBg,

    surfaceVariant = emberSurface2,
    onSurfaceVariant = emberMuted,

    outline = emberMuted,
    outlineVariant = emberDivider,

    inverseSurface = emberPrimary,
    inverseOnSurface = emberBg,
    inversePrimary = emberAccent,

    scrim = emberBg,

    surfaceContainerLowest = emberBg,
    surfaceContainerLow = emberSurface,
    surfaceContainer = emberSurface2,
    surfaceContainerHigh = emberElev,
    surfaceContainerHighest = emberElev,
    surfaceDim = emberBg,
    surfaceBright = emberSurface2,
)

@Composable
fun HelloTheme(
    // Phase 0: dark-only. isSystemInDarkTheme() is intentionally ignored;
    // the parameter is kept for call-site compatibility but has no effect.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Always use the Ember dark scheme. Light scheme is deferred (Phase 5).
    val colorScheme = darkScheme
    val semanticColors = darkSemanticColors()

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
