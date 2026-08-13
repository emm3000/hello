package com.emm.hello.core.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Ember Dark palette — Phase 0 tokens
// Hex and RGBA values are the frozen Ember dark palette; treat this file as their source of truth.
// NOTE: emberFaint uses 0.55 opacity (bumped from designer's 0.40 for WCAG AA).
// ─────────────────────────────────────────────────────────────────────────────

val emberBg = Color(0xFF0F0E0C)
val emberSurface = Color(0xFF1A1815)
val emberSurface2 = Color(0xFF26231E)
val emberElev = Color(0xFF3A352D)

// rgba(244,239,230,0.08) → 0x14 alpha  (round(0.08 * 255) = 20 = 0x14)
val emberDivider = Color(red = 244, green = 239, blue = 230, alpha = 20)

val emberOnBg = Color(0xFFE8E1D0)
val emberPrimary = Color(0xFFF4EFE6)
val emberMuted = Color(0xFF9C9079)
val emberHint = Color(0xFFCEBD9A)

// rgba(244,239,230,0.55) → 0x8C alpha  (round(0.55 * 255) = 140 = 0x8C)
val emberFaint = Color(red = 244, green = 239, blue = 230, alpha = 140)

val emberAccent = Color(0xFFCC7A4A)

// rgba(204,122,74,0.14) → 0x24 alpha   (round(0.14 * 255) = 36 = 0x24)
val emberAccentSoft = Color(red = 204, green = 122, blue = 74, alpha = 36)

val emberGood = Color(0xFF7BAE8B)
val emberWarn = Color(0xFFD49765)
val emberBad = Color(0xFFC16B5E)

// Soft tones at 0.12 alpha → 0x1F  (round(0.12 * 255) = 31 = 0x1F)
val emberGoodSoft = Color(red = 123, green = 174, blue = 139, alpha = 31)
val emberWarnSoft = Color(red = 212, green = 151, blue = 101, alpha = 31)
val emberBadSoft = Color(red = 193, green = 107, blue = 94, alpha = 31)

// ─────────────────────────────────────────────────────────────────────────────
// Semantic color packs — mapped to Ember tokens.
// success/warning/destructive expose a (container, content, accent) triple so
// HBadge / HStatCard / Material3 tertiary+error roles can opt into the same
// palette without redefining colors per call site.
// ─────────────────────────────────────────────────────────────────────────────

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
