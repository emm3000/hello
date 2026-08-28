package com.emm.hello.newfeatures.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.emm.hello.core.ui.HCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emm.domain.study.ReviewGrade
import com.emm.hello.R
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.surfaceRaised
import kotlin.math.abs

private const val CARD_FLIP_DURATION_MS = 420
private const val CARD_CAMERA_DISTANCE_MULTIPLIER = 30f
private const val FRONT_FACE_MAX_ROTATION = 90f
private const val BACK_FACE_ROTATION = 180f
private const val GRADIENT_TRANSITION_DURATION_MS = 500
private const val BACK_FACE_TINT_BLEND_FRACTION = 0.15f
private const val BACK_FACE_TRANSITION_DURATION_MS = 400
private const val SWIPE_SHORT_THRESHOLD_FRACTION = 0.25f
private const val SWIPE_OVERLAY_MAX_ALPHA = 0.55f
private const val SWIPE_OVERLAY_ALPHA_GAIN = 1.4f

@Composable
fun FlippableCard(
    modifier: Modifier = Modifier,
    cardFace: CardFace,
    onClick: (CardFace) -> Unit,
    progress: Float = 0f,
    gradeEnabled: Boolean = false,
    onGradeSwipe: (ReviewGrade) -> Unit = {},
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

    var rawDragOffset by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val canSwipe = gradeEnabled && cardFace == CardFace.Back

    LaunchedEffect(cardFace) {
        if (cardFace == CardFace.Front) rawDragOffset = 0f
    }

    val animatedOffset by animateFloatAsState(
        targetValue = rawDragOffset,
        animationSpec = spring(),
        label = "dragOffset",
    )

    HCard(
        modifier = modifier
            .onSizeChanged { widthPx = it.width }
            .clickable(
                onClick = { onClick(cardFace) },
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            )
            .pointerInput(canSwipe) {
                if (!canSwipe) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val grade = computeSwipeGrade(rawDragOffset, widthPx.toFloat())
                        rawDragOffset = 0f
                        if (grade != null) onGradeSwipe(grade)
                    },
                    onDragCancel = { rawDragOffset = 0f },
                    onHorizontalDrag = { change, delta ->
                        rawDragOffset += delta
                        change.consume()
                    },
                )
            }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = CARD_CAMERA_DISTANCE_MULTIPLIER * density
                translationX = if (cardFace == CardFace.Back) animatedOffset else 0f
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
                    if (canSwipe) {
                        DragGradeOverlay(
                            dragOffsetPx = animatedOffset,
                            widthPx = widthPx,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.DragGradeOverlay(
    dragOffsetPx: Float,
    widthPx: Int,
) {
    if (widthPx <= 0) return
    val fraction = (dragOffsetPx / widthPx).coerceIn(-1f, 1f)
    val absFraction = abs(fraction)
    if (absFraction < SWIPE_SHORT_THRESHOLD_FRACTION) return
    val grade = computeSwipeGrade(dragOffsetPx, widthPx.toFloat()) ?: return
    val tokens = swipeOverlayTokens(grade)
    val alpha = (absFraction * SWIPE_OVERLAY_ALPHA_GAIN).coerceAtMost(SWIPE_OVERLAY_MAX_ALPHA)
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(tokens.container.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(tokens.labelRes),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = tokens.content,
        )
    }
}

private data class SwipeOverlayTokens(
    val container: Color,
    val content: Color,
    val labelRes: Int,
)

@Composable
private fun swipeOverlayTokens(grade: ReviewGrade): SwipeOverlayTokens {
    val container = surfaceRaised
    val content = ink
    return if (grade == ReviewGrade.AGAIN) {
        SwipeOverlayTokens(container, content, R.string.study_grade_forgot)
    } else {
        SwipeOverlayTokens(container, content, R.string.study_grade_knew)
    }
}

internal fun computeSwipeGrade(
    dragOffsetPx: Float,
    widthPx: Float,
): ReviewGrade? {
    if (widthPx <= 0f) return null
    val fraction = dragOffsetPx / widthPx
    if (abs(fraction) < SWIPE_SHORT_THRESHOLD_FRACTION) return null
    return if (fraction < 0) ReviewGrade.AGAIN else ReviewGrade.GOOD
}

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
