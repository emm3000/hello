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

// ─────────────────────────────────────────────────────────────────────────────
// shadcn/ui — Neutral theme mapped to Material 3 color roles
//
// Mapping strategy:
//   M3 primary          → shadcn --primary (near-black / near-white)
//   M3 secondary        → shadcn --secondary (light gray)
//   M3 tertiary         → shadcn success green
//   M3 error            → shadcn --destructive (red)
//   M3 background/surface → shadcn --background
//   M3 surfaceVariant   → shadcn --muted (light gray bg)
//   M3 surfaceContainer → shadcn --card
//   M3 outline          → shadcn --ring (subtle ring)
//   M3 outlineVariant   → shadcn --border
// ─────────────────────────────────────────────────────────────────────────────

private val lightScheme = lightColorScheme(
    semanticColors = lightSemanticColors()
)

private fun lightColorScheme(
    semanticColors: HelloSemanticColors,
) = materialLightColorScheme(
    // ── Primary (black button in shadcn) ──────────────────────────────────────
    primary = shadcnPrimary,
    onPrimary = shadcnPrimaryFg,
    primaryContainer = shadcnSecondary,
    onPrimaryContainer = shadcnPrimary,

    // ── Secondary (soft gray, "secondary" variant) ────────────────────────────
    secondary = shadcnMuted,
    onSecondary = shadcnForeground,
    secondaryContainer = shadcnSecondary,
    onSecondaryContainer = shadcnSecondaryFg,

    // ── Tertiary → success green ───────────────────────────────────────────────
    tertiary = semanticColors.success.accent,
    onTertiary = shadcnWhite,
    tertiaryContainer = semanticColors.success.container,
    onTertiaryContainer = semanticColors.success.content,

    // ── Error → destructive red ───────────────────────────────────────────────
    error = semanticColors.destructive.accent,
    onError = shadcnDestructiveFg,
    errorContainer = semanticColors.destructive.container,
    onErrorContainer = semanticColors.destructive.content,

    // ── Background & Surface ──────────────────────────────────────────────────
    background = shadcnBackground,
    onBackground = shadcnForeground,
    surface = shadcnBackground,
    onSurface = shadcnForeground,

    // ── Surface variants → muted / card tones ────────────────────────────────
    surfaceVariant = shadcnMuted,
    onSurfaceVariant = shadcnMutedFg,

    // ── Borders & rings ───────────────────────────────────────────────────────
    outline = shadcnRing,
    outlineVariant = shadcnBorder,

    // ── Inverse ───────────────────────────────────────────────────────────────
    inverseSurface = shadcnPrimary,
    inverseOnSurface = shadcnPrimaryFg,
    inversePrimary = shadcnDarkPrimary,

    scrim = shadcnBlack,

    // ── Surface containers → card / popover tones ────────────────────────────
    surfaceContainerLowest = shadcnWhite,
    surfaceContainerLow = shadcnBackground, // pure white
    surfaceContainer = shadcnSecondary, // #F5F5F5
    surfaceContainerHigh = shadcnBorder, // #E5E5E5
    surfaceContainerHighest = shadcnInput, // #E5E5E5
    surfaceDim = shadcnBorder,
    surfaceBright = shadcnWhite,
)

private val darkScheme = darkColorScheme(
    semanticColors = darkSemanticColors()
)

private fun darkColorScheme(
    semanticColors: HelloSemanticColors,
) = materialDarkColorScheme(
    // ── Primary (near-white in dark mode) ─────────────────────────────────────
    primary = shadcnDarkPrimary,
    onPrimary = shadcnDarkPrimaryFg,
    primaryContainer = shadcnDarkSecondary,
    onPrimaryContainer = shadcnDarkPrimary,

    // ── Secondary ─────────────────────────────────────────────────────────────
    secondary = shadcnDarkMuted,
    onSecondary = shadcnDarkForeground,
    secondaryContainer = shadcnDarkSecondary,
    onSecondaryContainer = shadcnDarkSecondaryFg,

    // ── Tertiary → success green ───────────────────────────────────────────────
    tertiary = semanticColors.success.accent,
    onTertiary = shadcnBlack,
    tertiaryContainer = semanticColors.success.container,
    onTertiaryContainer = semanticColors.success.content,

    // ── Error → destructive red ───────────────────────────────────────────────
    error = semanticColors.destructive.accent,
    onError = shadcnDarkDestructiveFg,
    errorContainer = semanticColors.destructive.container,
    onErrorContainer = semanticColors.destructive.content,

    // ── Background & Surface ──────────────────────────────────────────────────
    background = shadcnDarkBackground,
    onBackground = shadcnDarkForeground,
    surface = shadcnDarkBackground,
    onSurface = shadcnDarkForeground,

    // ── Surface variants → muted tones ────────────────────────────────────────
    surfaceVariant = shadcnDarkMuted,
    onSurfaceVariant = shadcnDarkMutedFg,

    // ── Borders & rings ───────────────────────────────────────────────────────
    outline = shadcnDarkRing,
    outlineVariant = shadcnDarkBorder,

    // ── Inverse ───────────────────────────────────────────────────────────────
    inverseSurface = shadcnDarkPrimary,
    inverseOnSurface = shadcnDarkPrimaryFg,
    inversePrimary = shadcnPrimary,

    scrim = shadcnBlack,

    // ── Surface containers → card tones ──────────────────────────────────────
    surfaceContainerLowest = shadcnBlack,
    surfaceContainerLow = shadcnDarkBackground, // #0A0A0A
    surfaceContainer = shadcnDarkCard, // #171717
    surfaceContainerHigh = shadcnDarkSecondary, // #262626
    surfaceContainerHighest = shadcnDarkAccent, // #3F3F3F
    surfaceDim = shadcnDarkBackground,
    surfaceBright = shadcnDarkCard,
)

@Composable
fun HelloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled by default — we want exact shadcn colors
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
