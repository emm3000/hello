package com.emm.hello.core.theme

import androidx.compose.ui.graphics.Color

/*
 * shadcn/ui "Neutral" palette (New York style). Values converted from OKLCH → sRGB hex.
 *
 * Light mode:
 *   --background  oklch(1 0 0)              #FFFFFF
 *   --foreground  oklch(0.145 0 0)          #252525
 *   --primary     oklch(0.205 0 0)          #343434
 *   --secondary   oklch(0.97 0 0)           #F7F7F7
 *   --muted       oklch(0.97 0 0)           #F7F7F7
 *   --accent      oklch(0.97 0 0)           #F7F7F7
 *   --destructive oklch(0.577 0.245 27.325) #E5484D
 *   --border      oklch(0.922 0 0)          #EBEBEB
 *   --input       oklch(0.922 0 0)          #EBEBEB
 *   --ring        oklch(0.708 0 0)          #B5B5B5
 *
 * Dark mode:
 *   --background  oklch(0.145 0 0)          #252525
 *   --foreground  oklch(0.985 0 0)          #FAFAFA
 *   --primary     oklch(0.922 0 0)          #EBEBEB
 *   --secondary   oklch(0.269 0 0)          #404040
 *   --muted       oklch(0.269 0 0)          #404040
 *   --accent      oklch(0.371 0 0)          #5C5C5C
 *   --destructive oklch(0.704 0.191 22.216) #F2555A
 *   --border      oklch(1 0 0 / 10%)        #FFFFFF1A
 *   --input       oklch(1 0 0 / 15%)        #FFFFFF26
 *   --ring        oklch(0.556 0 0)          #8C8C8C
 */

val shadcnWhite = Color(0xFFFFFFFF)
val shadcnBlack = Color(0xFF000000)

val shadcnBackground = Color(0xFFFFFFFF)
val shadcnForeground = Color(0xFF0A0A0A)
val shadcnCard = Color(0xFFFFFFFF)
val shadcnCardFg = Color(0xFF0A0A0A)

val shadcnPrimary = Color(0xFF171717)
val shadcnPrimaryFg = Color(0xFFFAFAFA)

val shadcnSecondary = Color(0xFFF5F5F5)
val shadcnSecondaryFg = Color(0xFF171717)

val shadcnMuted = Color(0xFFF5F5F5)
val shadcnMutedFg = Color(0xFF737373)

val shadcnAccent = Color(0xFFF5F5F5)
val shadcnAccentFg = Color(0xFF171717)

val shadcnDestructive = Color(0xFFEF4444)
val shadcnDestructiveFg = Color(0xFFFAFAFA)

val shadcnBorder = Color(0xFFE5E5E5)
val shadcnInput = Color(0xFFE5E5E5)
val shadcnRing = Color(0xFFB3B3B3)

val shadcnPopover = Color(0xFFFFFFFF)
val shadcnPopoverFg = Color(0xFF0A0A0A)

val shadcnDarkBackground = Color(0xFF0A0A0A)
val shadcnDarkForeground = Color(0xFFFAFAFA)
val shadcnDarkCard = Color(0xFF171717)
val shadcnDarkCardFg = Color(0xFFFAFAFA)

val shadcnDarkPrimary = Color(0xFFFAFAFA)
val shadcnDarkPrimaryFg = Color(0xFF171717)

val shadcnDarkSecondary = Color(0xFF262626)
val shadcnDarkSecondaryFg = Color(0xFFFAFAFA)

val shadcnDarkMuted = Color(0xFF262626)
val shadcnDarkMutedFg = Color(0xFFA3A3A3)

val shadcnDarkAccent = Color(0xFF3F3F3F)
val shadcnDarkAccentFg = Color(0xFFFAFAFA)

val shadcnDarkDestructive = Color(0xFFF87171)
val shadcnDarkDestructiveFg = Color(0xFFFAFAFA)

val shadcnDarkBorder = Color(0x1AFFFFFF)
val shadcnDarkInput = Color(0x26FFFFFF)
val shadcnDarkRing = Color(0xFF737373)

val shadcnDarkPopover = Color(0xFF1C1C1C)
val shadcnDarkPopoverFg = Color(0xFFFAFAFA)

val shadcnErrorContainer = Color(0xFFFEE2E2)
val shadcnOnErrorContainer = Color(0xFF991B1B)

val shadcnDarkErrorContainer = Color(0xFF7F1D1D)
val shadcnDarkOnErrorContainer = Color(0xFFFECACA)

val shadcnSuccessContainer = Color(0xFFDCFCE7)
val shadcnOnSuccessContainer = Color(0xFF166534)
val shadcnSuccess = Color(0xFF16A34A)

val shadcnDarkSuccessContainer = Color(0xFF14532D)
val shadcnDarkOnSuccessContainer = Color(0xFFBBF7D0)
val shadcnDarkSuccess = Color(0xFF4ADE80)

val shadcnWarningContainer = Color(0xFFFEF9C3)
val shadcnOnWarningContainer = Color(0xFF854D0E)
val shadcnWarning = Color(0xFFCA8A04)

val shadcnDarkWarningContainer = Color(0xFF713F12)
val shadcnDarkOnWarningContainer = Color(0xFFFEF08A)
val shadcnDarkWarning = Color(0xFFFACC15)

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
