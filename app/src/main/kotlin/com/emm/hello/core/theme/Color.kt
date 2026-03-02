package com.emm.hello.core.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
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
// ─────────────────────────────────────────────────────────────────────────────

// ── Neutrals (shared) ────────────────────────────────────────────────────────
val ShadcnWhite        = Color(0xFFFFFFFF)
val ShadcnBlack        = Color(0xFF000000)

// Light palette
val ShadcnBackground   = Color(0xFFFFFFFF)   // --background
val ShadcnForeground   = Color(0xFF0A0A0A)   // --foreground  (oklch 0.145)
val ShadcnCard         = Color(0xFFFFFFFF)   // --card
val ShadcnCardFg       = Color(0xFF0A0A0A)   // --card-foreground

val ShadcnPrimary      = Color(0xFF171717)   // --primary      (oklch 0.205)
val ShadcnPrimaryFg    = Color(0xFFFAFAFA)   // --primary-foreground

val ShadcnSecondary    = Color(0xFFF5F5F5)   // --secondary    (oklch 0.97)
val ShadcnSecondaryFg  = Color(0xFF171717)   // --secondary-foreground

val ShadcnMuted        = Color(0xFFF5F5F5)   // --muted
val ShadcnMutedFg      = Color(0xFF737373)   // --muted-foreground (oklch 0.556)

val ShadcnAccent       = Color(0xFFF5F5F5)   // --accent
val ShadcnAccentFg     = Color(0xFF171717)   // --accent-foreground

val ShadcnDestructive  = Color(0xFFEF4444)   // --destructive  (oklch 0.577 0.245 27)
val ShadcnDestructiveFg = Color(0xFFFAFAFA)  // --destructive-foreground

val ShadcnBorder       = Color(0xFFE5E5E5)   // --border       (oklch 0.922)
val ShadcnInput        = Color(0xFFE5E5E5)   // --input
val ShadcnRing         = Color(0xFFB3B3B3)   // --ring         (oklch 0.708)

// Popover (same as card in Neutral)
val ShadcnPopover      = Color(0xFFFFFFFF)
val ShadcnPopoverFg    = Color(0xFF0A0A0A)

// Dark palette
val ShadcnDarkBackground  = Color(0xFF0A0A0A)  // --background dark
val ShadcnDarkForeground  = Color(0xFFFAFAFA)  // --foreground dark
val ShadcnDarkCard        = Color(0xFF171717)  // --card dark    (oklch 0.205)
val ShadcnDarkCardFg      = Color(0xFFFAFAFA)

val ShadcnDarkPrimary     = Color(0xFFFAFAFA)  // --primary dark  (oklch 0.922)
val ShadcnDarkPrimaryFg   = Color(0xFF171717)  // --primary-foreground dark

val ShadcnDarkSecondary   = Color(0xFF262626)  // --secondary dark (oklch 0.269)
val ShadcnDarkSecondaryFg = Color(0xFFFAFAFA)

val ShadcnDarkMuted       = Color(0xFF262626)  // --muted dark
val ShadcnDarkMutedFg     = Color(0xFFA3A3A3)  // --muted-foreground dark (oklch 0.708)

val ShadcnDarkAccent      = Color(0xFF3F3F3F)  // --accent dark  (oklch 0.371)
val ShadcnDarkAccentFg    = Color(0xFFFAFAFA)

val ShadcnDarkDestructive = Color(0xFFF87171)  // --destructive dark (oklch 0.704 0.191 22)
val ShadcnDarkDestructiveFg = Color(0xFFFAFAFA)

val ShadcnDarkBorder      = Color(0x1AFFFFFF)  // --border dark  oklch(1 0 0 / 10%)
val ShadcnDarkInput       = Color(0x26FFFFFF)  // --input dark   oklch(1 0 0 / 15%)
val ShadcnDarkRing        = Color(0xFF737373)  // --ring dark    (oklch 0.556)

val ShadcnDarkPopover     = Color(0xFF1C1C1C)
val ShadcnDarkPopoverFg   = Color(0xFFFAFAFA)

// ── Destructive / Error shades ────────────────────────────────────────────────
// Error container light (soft red, used in HAlert Destructive)
val ShadcnErrorContainer  = Color(0xFFFEE2E2) // Tailwind red-100
val ShadcnOnErrorContainer = Color(0xFF991B1B) // Tailwind red-800

// Error container dark
val ShadcnDarkErrorContainer  = Color(0xFF7F1D1D) // Tailwind red-900
val ShadcnDarkOnErrorContainer = Color(0xFFFECACA) // Tailwind red-200

// ── Success / Tertiary shades ─────────────────────────────────────────────────
// Using green-based tokens for HAlert Success and HBadge Success
val ShadcnSuccessContainer    = Color(0xFFDCFCE7) // Tailwind green-100
val ShadcnOnSuccessContainer  = Color(0xFF166534) // Tailwind green-800
val ShadcnSuccess             = Color(0xFF16A34A) // Tailwind green-600

val ShadcnDarkSuccessContainer   = Color(0xFF14532D) // Tailwind green-900
val ShadcnDarkOnSuccessContainer = Color(0xFFBBF7D0) // Tailwind green-200
val ShadcnDarkSuccess            = Color(0xFF4ADE80) // Tailwind green-400

// ── Warning shades ────────────────────────────────────────────────────────────
val ShadcnWarningContainer    = Color(0xFFFEF9C3) // Tailwind yellow-100
val ShadcnOnWarningContainer  = Color(0xFF854D0E) // Tailwind yellow-800
val ShadcnWarning             = Color(0xFFCA8A04) // Tailwind yellow-600

val ShadcnDarkWarningContainer   = Color(0xFF713F12) // Tailwind yellow-900
val ShadcnDarkOnWarningContainer = Color(0xFFFEF08A) // Tailwind yellow-200
val ShadcnDarkWarning            = Color(0xFFFACC15) // Tailwind yellow-400