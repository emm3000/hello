package com.emm.hello.core.theme

import androidx.compose.ui.graphics.Color

val pageBackground: Color = Color(0xFFF4F3F1)
val surface: Color = Color(0xFFFBFAF9)
val surfaceRaised: Color = Color(0xFFE9E7E3)

val ink: Color = Color(0xFF15141A)
val onInk: Color = Color(0xFFF4F3F1)
val inkMuted: Color = Color(0xFF6F6D75)
val inkFaint: Color = ink.copy(alpha = 0.45f)
val hairline: Color = ink.copy(alpha = 0.12f)
val outline: Color = ink.copy(alpha = 0.30f)

val cardPeach: Color = Color(0xFFF5C9A8)
val cardMint: Color = Color(0xFFBFE3CB)
val cardPeriwinkle: Color = Color(0xFFC6D3F5)
val cardLavender: Color = Color(0xFFDCC8F0)
val cardHues: List<Color> = listOf(cardPeach, cardMint, cardPeriwinkle, cardLavender)

val successInk: Color = Color(0xFF2F6B4F)
val successContainer: Color = Color(0xFFDDEBE2)
val warningInk: Color = Color(0xFF8A5A12)
val warningContainer: Color = Color(0xFFF3E6C8)
val destructiveInk: Color = Color(0xFFA33A3A)
val destructiveContainer: Color = Color(0xFFF3DADA)

internal val successSemanticColor = HelloSemanticColor(
    container = successContainer,
    content = successInk,
    accent = successInk,
)

internal val warningSemanticColor = HelloSemanticColor(
    container = warningContainer,
    content = warningInk,
    accent = warningInk,
)

internal val destructiveSemanticColor = HelloSemanticColor(
    container = destructiveContainer,
    content = destructiveInk,
    accent = destructiveInk,
)

internal fun helloSemanticColors(): HelloSemanticColors = HelloSemanticColors(
    success = successSemanticColor,
    warning = warningSemanticColor,
    destructive = destructiveSemanticColor,
)
