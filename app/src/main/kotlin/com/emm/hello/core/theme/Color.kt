package com.emm.hello.core.theme

import androidx.compose.ui.graphics.Color

val instrumentBg = Color(0xFF08090A)
val instrumentSurface = Color(0xFF101315)
val instrumentSurface2 = Color(0xFF181B1E)
val instrumentElev = Color(0xFF202427)

val instrumentDivider = Color.White.copy(alpha = 0.08f)

val instrumentOnBg = Color(0xFFDDE3E8)
val instrumentPrimary = Color(0xFFF2F5F7)
val instrumentMuted = Color(0xFF79838B)

// The design brief specifies 0.45, which measures 4.47:1 against instrumentBg and fails
// WCAG AA for normal text. 0.50 measures 5.31:1.
val instrumentFaint = Color.White.copy(alpha = 0.50f)

val instrumentAccent = Color(0xFF6BA3D6)
val instrumentOnAccent = Color(0xFF06181F)
val instrumentAccentSoft = instrumentAccent.copy(alpha = 0.14f)

val instrumentGood = Color(0xFF7FA98F)
val instrumentWarn = Color(0xFFC2A16A)
val instrumentBad = Color(0xFFC67F79)

val instrumentGoodSoft = instrumentGood.copy(alpha = 0.12f)
val instrumentWarnSoft = instrumentWarn.copy(alpha = 0.12f)
val instrumentBadSoft = instrumentBad.copy(alpha = 0.12f)

internal val instrumentSuccessSemanticColor = HelloSemanticColor(
    container = instrumentGoodSoft,
    content = instrumentGood,
    accent = instrumentGood,
)

internal val instrumentWarningSemanticColor = HelloSemanticColor(
    container = instrumentWarnSoft,
    content = instrumentWarn,
    accent = instrumentWarn,
)

internal val instrumentDestructiveSemanticColor = HelloSemanticColor(
    container = instrumentBadSoft,
    content = instrumentBad,
    accent = instrumentBad,
)

internal fun instrumentSemanticColors(): HelloSemanticColors = HelloSemanticColors(
    success = instrumentSuccessSemanticColor,
    warning = instrumentWarningSemanticColor,
    destructive = instrumentDestructiveSemanticColor,
)
