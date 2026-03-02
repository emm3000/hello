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
    primary = ShadcnPrimary,
    onPrimary = ShadcnPrimaryFg,
    primaryContainer = ShadcnSecondary,
    onPrimaryContainer = ShadcnPrimary,

    // ── Secondary (soft gray, "secondary" variant) ────────────────────────────
    secondary = ShadcnMuted,
    onSecondary = ShadcnForeground,
    secondaryContainer = ShadcnSecondary,
    onSecondaryContainer = ShadcnSecondaryFg,

    // ── Tertiary → success green ───────────────────────────────────────────────
    tertiary = ShadcnSuccess,
    onTertiary = ShadcnWhite,
    tertiaryContainer = ShadcnSuccessContainer,
    onTertiaryContainer = ShadcnOnSuccessContainer,

    // ── Error → destructive red ───────────────────────────────────────────────
    error = ShadcnDestructive,
    onError = ShadcnDestructiveFg,
    errorContainer = ShadcnErrorContainer,
    onErrorContainer = ShadcnOnErrorContainer,

    // ── Background & Surface ──────────────────────────────────────────────────
    background = ShadcnBackground,
    onBackground = ShadcnForeground,
    surface = ShadcnBackground,
    onSurface = ShadcnForeground,

    // ── Surface variants → muted / card tones ────────────────────────────────
    surfaceVariant = ShadcnMuted,
    onSurfaceVariant = ShadcnMutedFg,

    // ── Borders & rings ───────────────────────────────────────────────────────
    outline = ShadcnRing,
    outlineVariant = ShadcnBorder,

    // ── Inverse ───────────────────────────────────────────────────────────────
    inverseSurface = ShadcnPrimary,
    inverseOnSurface = ShadcnPrimaryFg,
    inversePrimary = ShadcnDarkPrimary,

    scrim = ShadcnBlack,

    // ── Surface containers → card / popover tones ────────────────────────────
    surfaceContainerLowest = ShadcnWhite,
    surfaceContainerLow = ShadcnBackground, // pure white
    surfaceContainer = ShadcnSecondary, // #F5F5F5
    surfaceContainerHigh = ShadcnBorder, // #E5E5E5
    surfaceContainerHighest = ShadcnInput, // #E5E5E5
    surfaceDim = ShadcnBorder,
    surfaceBright = ShadcnWhite,
)

private val darkScheme = darkColorScheme(
    // ── Primary (near-white in dark mode) ─────────────────────────────────────
    primary = ShadcnDarkPrimary,
    onPrimary = ShadcnDarkPrimaryFg,
    primaryContainer = ShadcnDarkSecondary,
    onPrimaryContainer = ShadcnDarkPrimary,

    // ── Secondary ─────────────────────────────────────────────────────────────
    secondary = ShadcnDarkMuted,
    onSecondary = ShadcnDarkForeground,
    secondaryContainer = ShadcnDarkSecondary,
    onSecondaryContainer = ShadcnDarkSecondaryFg,

    // ── Tertiary → success green ───────────────────────────────────────────────
    tertiary = ShadcnDarkSuccess,
    onTertiary = ShadcnBlack,
    tertiaryContainer = ShadcnDarkSuccessContainer,
    onTertiaryContainer = ShadcnDarkOnSuccessContainer,

    // ── Error → destructive red ───────────────────────────────────────────────
    error = ShadcnDarkDestructive,
    onError = ShadcnDarkDestructiveFg,
    errorContainer = ShadcnDarkErrorContainer,
    onErrorContainer = ShadcnDarkOnErrorContainer,

    // ── Background & Surface ──────────────────────────────────────────────────
    background = ShadcnDarkBackground,
    onBackground = ShadcnDarkForeground,
    surface = ShadcnDarkBackground,
    onSurface = ShadcnDarkForeground,

    // ── Surface variants → muted tones ────────────────────────────────────────
    surfaceVariant = ShadcnDarkMuted,
    onSurfaceVariant = ShadcnDarkMutedFg,

    // ── Borders & rings ───────────────────────────────────────────────────────
    outline = ShadcnDarkRing,
    outlineVariant = ShadcnDarkBorder,

    // ── Inverse ───────────────────────────────────────────────────────────────
    inverseSurface = ShadcnDarkPrimary,
    inverseOnSurface = ShadcnDarkPrimaryFg,
    inversePrimary = ShadcnPrimary,

    scrim = ShadcnBlack,

    // ── Surface containers → card tones ──────────────────────────────────────
    surfaceContainerLowest = ShadcnBlack,
    surfaceContainerLow = ShadcnDarkBackground, // #0A0A0A
    surfaceContainer = ShadcnDarkCard, // #171717
    surfaceContainerHigh = ShadcnDarkSecondary, // #262626
    surfaceContainerHighest = ShadcnDarkAccent, // #3F3F3F
    surfaceDim = ShadcnDarkBackground,
    surfaceBright = ShadcnDarkCard,
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
        typography = Typography,
        content = content,
    )
}
