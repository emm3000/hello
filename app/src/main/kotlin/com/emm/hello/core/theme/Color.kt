package com.emm.hello.core.theme

import androidx.compose.ui.graphics.Color

val emberBg = Color(0xFF08090A)
val emberSurface = Color(0xFF101315)
val emberSurface2 = Color(0xFF181B1E)
val emberElev = Color(0xFF202427)

val emberDivider = Color.White.copy(alpha = 0.08f)

val emberOnBg = Color(0xFFDDE3E8)
val emberPrimary = Color(0xFFF2F5F7)
val emberMuted = Color(0xFF79838B)

// The design brief specifies 0.45, which measures 4.47:1 against emberBg and fails
// WCAG AA for normal text. 0.50 measures 5.31:1.
val emberFaint = Color.White.copy(alpha = 0.50f)

val emberAccent = Color(0xFF6BA3D6)
val emberOnAccent = Color(0xFF06181F)
val emberAccentSoft = emberAccent.copy(alpha = 0.14f)

val emberGood = Color(0xFF7FA98F)
val emberWarn = Color(0xFFC2A16A)
val emberBad = Color(0xFFC67F79)

val emberGoodSoft = emberGood.copy(alpha = 0.12f)
val emberWarnSoft = emberWarn.copy(alpha = 0.12f)
val emberBadSoft = emberBad.copy(alpha = 0.12f)

internal val emberSuccessSemanticColor = HelloSemanticColor(
    container = emberGoodSoft,
    content = emberGood,
    accent = emberGood,
)

internal val emberWarningSemanticColor = HelloSemanticColor(
    container = emberWarnSoft,
    content = emberWarn,
    accent = emberWarn,
)

internal val emberDestructiveSemanticColor = HelloSemanticColor(
    container = emberBadSoft,
    content = emberBad,
    accent = emberBad,
)

internal fun emberSemanticColors(): HelloSemanticColors = HelloSemanticColors(
    success = emberSuccessSemanticColor,
    warning = emberWarningSemanticColor,
    destructive = emberDestructiveSemanticColor,
)
