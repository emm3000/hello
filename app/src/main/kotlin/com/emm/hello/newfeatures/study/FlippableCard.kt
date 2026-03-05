package com.emm.hello.newfeatures.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun FlippableCard(
    modifier: Modifier = Modifier,
    cardFace: CardFace,
    onClick: (CardFace) -> Unit,
    progress: Float = 0f,
    onFinished: (Float) -> Unit = {},
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = cardFace.angle,
        animationSpec = tween(durationMillis = 600),
        label = "cardRotation",
        finishedListener = onFinished,
    )

    val gradientBrush = rememberDynamicStudyGradient(
        progress = progress,
        cardFace = cardFace,
    )

    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .clickable(
                onClick = { onClick(cardFace) },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
        ),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            if (rotation <= 90f) {
                frontContent()
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                ) {
                    backContent()
                }
            }
        }
    }
}

// ── Dynamic gradient that evolves with session progress ──────────────────────

@Composable
fun rememberDynamicStudyGradient(
    progress: Float,
    cardFace: CardFace,
): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    // Opaque color palettes per theme — no alpha transparency
    val startColor1: Color
    val startColor2: Color
    val endColor1: Color
    val endColor2: Color

    if (isDark) {
        // Dark: surface → surfaceContainerHigh (subtle contrast)
        startColor1 = colorScheme.surface
        startColor2 = colorScheme.surfaceContainerHigh
        endColor1 = colorScheme.surfaceContainerHigh
        endColor2 = colorScheme.surfaceContainer
    } else {
        // Light: use the container tones from shadcn for visible difference
        // surfaceContainerLowest (#FFFFFF) → surfaceContainer (#F5F5F5)
        // and towards surfaceContainerHigh (#E5E5E5) at full progress
        startColor1 = colorScheme.surfaceContainerLowest
        startColor2 = colorScheme.surfaceContainer
        endColor1 = colorScheme.surfaceContainer
        endColor2 = colorScheme.surfaceContainerHigh
    }

    // Animate the gradient colors based on progress (smooth transition per card)
    val animatedColor1 by animateColorAsState(
        targetValue = lerp(startColor1, endColor1, progress),
        animationSpec = tween(durationMillis = 500),
        label = "gradientColor1",
    )
    val animatedColor2 by animateColorAsState(
        targetValue = lerp(startColor2, endColor2, progress),
        animationSpec = tween(durationMillis = 500),
        label = "gradientColor2",
    )

    // Subtle tint shift when card is flipped to the back
    val backTint = if (isDark) {
        colorScheme.surfaceContainerHighest
    } else {
        colorScheme.surfaceContainerHigh
    }

    val finalColor1 by animateColorAsState(
        targetValue = if (cardFace == CardFace.Back) {
            lerp(animatedColor1, backTint, 0.15f)
        } else {
            animatedColor1
        },
        animationSpec = tween(durationMillis = 400),
        label = "finalColor1",
    )
    val finalColor2 by animateColorAsState(
        targetValue = if (cardFace == CardFace.Back) {
            lerp(animatedColor2, backTint, 0.15f)
        } else {
            animatedColor2
        },
        animationSpec = tween(durationMillis = 400),
        label = "finalColor2",
    )

    return remember(finalColor1, finalColor2) {
        Brush.verticalGradient(colors = listOf(finalColor1, finalColor2))
    }
}

// ── Color utilities ─────────────────────────────────────────────────────────

private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clampedFraction,
        green = start.green + (end.green - start.green) * clampedFraction,
        blue = start.blue + (end.blue - start.blue) * clampedFraction,
        alpha = 1f, // always opaque
    )
}
