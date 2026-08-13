package com.emm.hello.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.darkColorScheme as materialDarkColorScheme

/*
 * Ember Dark color scheme. The app is dark-only by design;
 * the previous shadcn/light scheme was removed in Phase 5. Material 3 color
 * roles are wired to Ember tokens so any third-party Compose surface that
 * pulls from MaterialTheme.colorScheme inherits the palette automatically.
 */

private val darkScheme = darkColorScheme(
    semanticColors = emberSemanticColors(),
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
            semanticColors = emberSemanticColors(),
            content = content,
        )
    }
}
