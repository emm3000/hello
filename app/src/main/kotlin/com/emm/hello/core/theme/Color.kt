package com.emm.hello.core.theme

import androidx.compose.ui.graphics.Color

// ----------------------------------------------------------------------------─
// shadcn/ui — "Neutral" base color palette (New York style)
// CSS variables converted from OKLCH → sRGB hex
//
// Light mode reference:
//   --background:   oklch(1 0 0)        → #FFFFFF
//   --foreground:   oklch(0.145 0 0)    → #252525
//   --primary:      oklch(0.205 0 0)    → #343434
//   --secondary:    oklch(0.97 0 0)     → #F7F7F7
//   --muted:        oklch(0.97 0 0)     → #F7F7F7
//   --accent:       oklch(0.97 0 0)     → #F7F7F7
//   --destructive:  oklch(0.577 0.245 27.325) → #E5484D
//   --border:       oklch(0.922 0 0)    → #EBEBEB
//   --input:        oklch(0.922 0 0)    → #EBEBEB
//   --ring:         oklch(0.708 0 0)    → #B5B5B5
//
// Dark mode reference:
//   --background:   oklch(0.145 0 0)    → #252525
//   --foreground:   oklch(0.985 0 0)    → #FAFAFA
//   --primary:      oklch(0.922 0 0)    → #EBEBEB
//   --secondary:    oklch(0.269 0 0)    → #404040
//   --muted:        oklch(0.269 0 0)    → #404040
//   --accent:       oklch(0.371 0 0)    → #5C5C5C
//   --destructive:  oklch(0.704 0.191 22.216) → #F2555A
//   --border:       oklch(1 0 0 / 10%) → #FFFFFF1A
//   --input:        oklch(1 0 0 / 15%) → #FFFFFF26
//   --ring:         oklch(0.556 0 0)    → #8C8C8C
// ----------------------------------------------------------------------------─

// -- Neutrals (shared) --------------------------------------------------------
val shadcnWhite = Color(0xFFFFFFFF)
val shadcnBlack = Color(0xFF000000)

// Light palette
val shadcnBackground = Color(0xFFFFFFFF) // --background
val shadcnForeground = Color(0xFF0A0A0A) // --foreground  (oklch 0.145)
val shadcnCard = Color(0xFFFFFFFF) // --card
val shadcnCardFg = Color(0xFF0A0A0A) // --card-foreground

val shadcnPrimary = Color(0xFF171717) // --primary      (oklch 0.205)
val shadcnPrimaryFg = Color(0xFFFAFAFA) // --primary-foreground

val shadcnSecondary = Color(0xFFF5F5F5) // --secondary    (oklch 0.97)
val shadcnSecondaryFg = Color(0xFF171717) // --secondary-foreground

val shadcnMuted = Color(0xFFF5F5F5) // --muted
val shadcnMutedFg = Color(0xFF737373) // --muted-foreground (oklch 0.556)

val shadcnAccent = Color(0xFFF5F5F5) // --accent
val shadcnAccentFg = Color(0xFF171717) // --accent-foreground

val shadcnDestructive = Color(0xFFEF4444) // --destructive  (oklch 0.577 0.245 27)
val shadcnDestructiveFg = Color(0xFFFAFAFA) // --destructive-foreground

val shadcnBorder = Color(0xFFE5E5E5) // --border       (oklch 0.922)
val shadcnInput = Color(0xFFE5E5E5) // --input
val shadcnRing = Color(0xFFB3B3B3) // --ring         (oklch 0.708)

// Popover (same as card in Neutral)
val shadcnPopover = Color(0xFFFFFFFF)
val shadcnPopoverFg = Color(0xFF0A0A0A)

// Dark palette
val shadcnDarkBackground = Color(0xFF0A0A0A) // --background dark
val shadcnDarkForeground = Color(0xFFFAFAFA) // --foreground dark
val shadcnDarkCard = Color(0xFF171717) // --card dark    (oklch 0.205)
val shadcnDarkCardFg = Color(0xFFFAFAFA)

val shadcnDarkPrimary = Color(0xFFFAFAFA) // --primary dark  (oklch 0.922)
val shadcnDarkPrimaryFg = Color(0xFF171717) // --primary-foreground dark

val shadcnDarkSecondary = Color(0xFF262626) // --secondary dark (oklch 0.269)
val shadcnDarkSecondaryFg = Color(0xFFFAFAFA)

val shadcnDarkMuted = Color(0xFF262626) // --muted dark
val shadcnDarkMutedFg = Color(0xFFA3A3A3) // --muted-foreground dark (oklch 0.708)

val shadcnDarkAccent = Color(0xFF3F3F3F) // --accent dark  (oklch 0.371)
val shadcnDarkAccentFg = Color(0xFFFAFAFA)

val shadcnDarkDestructive = Color(0xFFF87171) // --destructive dark (oklch 0.704 0.191 22)
val shadcnDarkDestructiveFg = Color(0xFFFAFAFA)

val shadcnDarkBorder = Color(0x1AFFFFFF) // --border dark  oklch(1 0 0 / 10%)
val shadcnDarkInput = Color(0x26FFFFFF) // --input dark   oklch(1 0 0 / 15%)
val shadcnDarkRing = Color(0xFF737373) // --ring dark    (oklch 0.556)

val shadcnDarkPopover = Color(0xFF1C1C1C)
val shadcnDarkPopoverFg = Color(0xFFFAFAFA)

// -- Destructive / Error shades -----------------------------------------------
// Error container light (soft red, used in HAlert Destructive)
val shadcnErrorContainer = Color(0xFFFEE2E2) // Tailwind red-100
val shadcnOnErrorContainer = Color(0xFF991B1B) // Tailwind red-800

// Error container dark
val shadcnDarkErrorContainer = Color(0xFF7F1D1D) // Tailwind red-900
val shadcnDarkOnErrorContainer = Color(0xFFFECACA) // Tailwind red-200

// -- Success / Tertiary shades ------------------------------------------------
// Using green-based tokens for HAlert Success and HBadge Success
val shadcnSuccessContainer = Color(0xFFDCFCE7) // Tailwind green-100
val shadcnOnSuccessContainer = Color(0xFF166534) // Tailwind green-800
val shadcnSuccess = Color(0xFF16A34A) // Tailwind green-600

val shadcnDarkSuccessContainer = Color(0xFF14532D) // Tailwind green-900
val shadcnDarkOnSuccessContainer = Color(0xFFBBF7D0) // Tailwind green-200
val shadcnDarkSuccess = Color(0xFF4ADE80) // Tailwind green-400

// -- Warning shades -----------------------------------------------------------
val shadcnWarningContainer = Color(0xFFFEF9C3) // Tailwind yellow-100
val shadcnOnWarningContainer = Color(0xFF854D0E) // Tailwind yellow-800
val shadcnWarning = Color(0xFFCA8A04) // Tailwind yellow-600

val shadcnDarkWarningContainer = Color(0xFF713F12) // Tailwind yellow-900
val shadcnDarkOnWarningContainer = Color(0xFFFEF08A) // Tailwind yellow-200
val shadcnDarkWarning = Color(0xFFFACC15) // Tailwind yellow-400

internal val lightSuccessSemanticColor = HelloSemanticColor(
    container = shadcnSuccessContainer,
    content = shadcnOnSuccessContainer,
    accent = shadcnSuccess,
)

internal val lightWarningSemanticColor = HelloSemanticColor(
    container = shadcnWarningContainer,
    content = shadcnOnWarningContainer,
    accent = shadcnWarning,
)

internal val lightDestructiveSemanticColor = HelloSemanticColor(
    container = shadcnErrorContainer,
    content = shadcnOnErrorContainer,
    accent = shadcnDestructive,
)

internal val darkSuccessSemanticColor = HelloSemanticColor(
    container = shadcnDarkSuccessContainer,
    content = shadcnDarkOnSuccessContainer,
    accent = shadcnDarkSuccess,
)

internal val darkWarningSemanticColor = HelloSemanticColor(
    container = shadcnDarkWarningContainer,
    content = shadcnDarkOnWarningContainer,
    accent = shadcnDarkWarning,
)

internal val darkDestructiveSemanticColor = HelloSemanticColor(
    container = shadcnDarkErrorContainer,
    content = shadcnDarkOnErrorContainer,
    accent = shadcnDarkDestructive,
)

internal fun lightSemanticColors(): HelloSemanticColors = HelloSemanticColors(
    success = lightSuccessSemanticColor,
    warning = lightWarningSemanticColor,
    destructive = lightDestructiveSemanticColor,
)

internal fun darkSemanticColors(): HelloSemanticColors = HelloSemanticColors(
    success = darkSuccessSemanticColor,
    warning = darkWarningSemanticColor,
    destructive = darkDestructiveSemanticColor,
)
