package com.emm.hello.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
    tertiary = shadcnSuccess,
    onTertiary = shadcnWhite,
    tertiaryContainer = shadcnSuccessContainer,
    onTertiaryContainer = shadcnOnSuccessContainer,

    // ── Error → destructive red ───────────────────────────────────────────────
    error = shadcnDestructive,
    onError = shadcnDestructiveFg,
    errorContainer = shadcnErrorContainer,
    onErrorContainer = shadcnOnErrorContainer,

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
    tertiary = shadcnDarkSuccess,
    onTertiary = shadcnBlack,
    tertiaryContainer = shadcnDarkSuccessContainer,
    onTertiaryContainer = shadcnDarkOnSuccessContainer,

    // ── Error → destructive red ───────────────────────────────────────────────
    error = shadcnDarkDestructive,
    onError = shadcnDarkDestructiveFg,
    errorContainer = shadcnDarkErrorContainer,
    onErrorContainer = shadcnDarkOnErrorContainer,

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
        content = content,
    )
}
