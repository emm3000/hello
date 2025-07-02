package com.emm.hello.newfeatures.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun FlippableCard(
    modifier: Modifier = Modifier,
    cardFace: CardFace,
    onClick: (CardFace) -> Unit,
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit
) {
    // Anima el valor de la rotación cuando cambia el estado 'cardFace'.
    val rotation by animateFloatAsState(
        targetValue = cardFace.angle,
        animationSpec = tween(
            durationMillis = 600, // Duración de la animación
        ),
        label = "cardRotation"
    )

    Card(
        modifier = modifier
            .clickable(
                onClick = { onClick(cardFace) },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .graphicsLayer {
                // Aplica la rotación en el eje Y.
                rotationY = rotation
                // Aumenta la distancia de la cámara para un efecto 3D más realista.
                // El valor por defecto es 8.dp, un valor mayor da más perspectiva.
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(

        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Mostramos el contenido frontal o trasero según el ángulo de rotación.
            if (rotation <= 90f) {
                frontContent()
            } else {
                // El contenido trasero necesita ser rotado 180 grados para corregir
                // el efecto espejo causado por la rotación de la tarjeta principal.
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