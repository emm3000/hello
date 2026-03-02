package com.emm.hello.newfeatures.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    onFinished: (Float) -> Unit = {},
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = cardFace.angle,
        animationSpec = tween(
            durationMillis = 600, // Animation duration
        ),
        label = "cardRotation",
        finishedListener = onFinished,
    )

    val randomGradient = rememberThemedMinimalGradientBrush()

    Card(
        modifier = modifier
            .clickable(
                onClick = {
                    onClick(cardFace)
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .graphicsLayer {
                // Applies the Y-axis rotation.
                rotationY = rotation
                // Increases camera distance for a more realistic 3D effect.
                // Default value is 8.dp; a higher value gives more perspective.
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent, // Make card container transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(randomGradient) // Apply gradient to the Box
        ) {
            // Show front or back content based on the rotation angle.
            if (rotation <= 90f) {
                frontContent()
            } else {
                // Back content needs to be rotated 180 degrees to correct
                // the mirror effect caused by the main card's rotation.
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = 180f
                        }
                ) {
                    backContent()
                }
            }
        }
    }
}

@Composable
fun rememberThemedMinimalGradientBrush(): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val gradients = remember(colorScheme, isDark) {
        if (isDark) {
            listOf(
                listOf(colorScheme.surface, colorScheme.surfaceVariant),
                listOf(colorScheme.surfaceVariant, colorScheme.outline),
                listOf(colorScheme.background, colorScheme.surface),
                listOf(Color(0xFF1E1E1E), colorScheme.outline.copy(alpha = 0.5f))
            )
        } else {
            listOf(
                listOf(colorScheme.background, colorScheme.surfaceVariant),
                listOf(Color.White, colorScheme.primaryContainer.copy(alpha = 0.3f)),
                listOf(colorScheme.surface, colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                listOf(colorScheme.surfaceVariant, colorScheme.outline.copy(alpha = 0.1f))
            )
        }
    }

    val selected = remember(gradients) { gradients.random() }

    return remember(selected) {
        Brush.linearGradient(colors = selected)
    }
}
