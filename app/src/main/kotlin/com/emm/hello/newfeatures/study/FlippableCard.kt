package com.emm.hello.newfeatures.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.emm.hello.core.ui.HCard

private const val CARD_FLIP_DURATION_MS = 420
private const val CARD_CAMERA_DISTANCE_MULTIPLIER = 30f
private const val FRONT_FACE_MAX_ROTATION = 90f
private const val BACK_FACE_ROTATION = 180f
private const val GRADIENT_TRANSITION_DURATION_MS = 500
private const val BACK_FACE_TINT_BLEND_FRACTION = 0.15f
private const val BACK_FACE_TRANSITION_DURATION_MS = 400

@Composable
fun FlippableCard(
    modifier: Modifier = Modifier,
    cardFace: CardFace,
    onClick: (CardFace) -> Unit,
    progress: Float = 0f,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = cardFace.angle,
        animationSpec = tween(
            durationMillis = CARD_FLIP_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "cardRotation",
    )

    val gradientBrush = rememberDynamicStudyGradient(
        progress = progress,
        cardFace = cardFace,
    )

    val borderColor = MaterialTheme.colorScheme.outlineVariant

    HCard(
        modifier = modifier
            .clickable(
                onClick = { onClick(cardFace) },
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            )
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = CARD_CAMERA_DISTANCE_MULTIPLIER * density
            },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = 4.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            if (rotation <= FRONT_FACE_MAX_ROTATION) {
                frontContent()
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = BACK_FACE_ROTATION }
                ) {
                    backContent()
                }
            }
        }
    }
}

@Composable
fun rememberDynamicStudyGradient(
    progress: Float,
    cardFace: CardFace,
): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val startColor1: Color
    val startColor2: Color
    val endColor1: Color
    val endColor2: Color

    if (isDark) {
        startColor1 = colorScheme.surface
        startColor2 = colorScheme.surfaceContainerHigh
        endColor1 = colorScheme.surfaceContainerHigh
        endColor2 = colorScheme.surfaceContainer
    } else {
        startColor1 = colorScheme.surfaceContainerLowest
        startColor2 = colorScheme.surfaceContainer
        endColor1 = colorScheme.surfaceContainer
        endColor2 = colorScheme.surfaceContainerHigh
    }

    val animatedColor1 by animateColorAsState(
        targetValue = lerp(startColor1, endColor1, progress),
        animationSpec = tween(durationMillis = GRADIENT_TRANSITION_DURATION_MS),
        label = "gradientColor1",
    )
    val animatedColor2 by animateColorAsState(
        targetValue = lerp(startColor2, endColor2, progress),
        animationSpec = tween(durationMillis = GRADIENT_TRANSITION_DURATION_MS),
        label = "gradientColor2",
    )

    val backTint = if (isDark) {
        colorScheme.surfaceContainerHighest
    } else {
        colorScheme.surfaceContainerHigh
    }

    val finalColor1 by animateColorAsState(
        targetValue = if (cardFace == CardFace.Back) {
            lerp(animatedColor1, backTint, BACK_FACE_TINT_BLEND_FRACTION)
        } else {
            animatedColor1
        },
        animationSpec = tween(durationMillis = BACK_FACE_TRANSITION_DURATION_MS),
        label = "finalColor1",
    )
    val finalColor2 by animateColorAsState(
        targetValue = if (cardFace == CardFace.Back) {
            lerp(animatedColor2, backTint, BACK_FACE_TINT_BLEND_FRACTION)
        } else {
            animatedColor2
        },
        animationSpec = tween(durationMillis = BACK_FACE_TRANSITION_DURATION_MS),
        label = "finalColor2",
    )

    return remember(finalColor1, finalColor2) {
        Brush.verticalGradient(colors = listOf(finalColor1, finalColor2))
    }
}

private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clampedFraction,
        green = start.green + (end.green - start.green) * clampedFraction,
        blue = start.blue + (end.blue - start.blue) * clampedFraction,
        alpha = 1f,
    )
}
