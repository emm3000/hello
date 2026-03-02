package com.emm.hello.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Primitivo ───────────────────────────────────────────────────────────────

/**
 * Skeleton primitivo inspirado en shadcn/ui.
 *
 * Produce un rectángulo con shimmer animado. Especificar siempre
 * [modifier] con width/height explícitos.
 */
@Composable
fun HSkeleton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface

    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateX by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton_translateX",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 400f, 0f),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush),
    )
}

// ─── Presets de pantalla ─────────────────────────────────────────────────────

/** Skeleton de un DeckItem en el Dashboard. */
@Composable
fun DeckItemSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            HSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(12.dp),
            )
        }
        HSkeleton(
            modifier = Modifier
                .width(24.dp)
                .height(24.dp),
            cornerRadius = 4.dp,
        )
    }
}

/** Skeleton de la pantalla Dashboard con N DeckItems. */
@Composable
fun DashboardSkeleton(count: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HSkeleton(Modifier.width(80.dp).height(18.dp))
            Spacer(Modifier.weight(1f))
            HSkeleton(Modifier.width(100.dp).height(14.dp))
        }
        Spacer(Modifier.height(8.dp))
        repeat(count) {
            DeckItemSkeleton()
            HSeparator()
        }
    }
}

/** Skeleton genérico de líneas de texto. */
@Composable
fun TextSkeleton(
    lines: Int = 3,
    modifier: Modifier = Modifier,
    lastLineWidth: Float = 0.6f,
) {
    Column(modifier = modifier) {
        repeat(lines) { index ->
            val fraction = if (index == lines - 1) lastLineWidth else 1f
            HSkeleton(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(14.dp),
            )
            if (index < lines - 1) Spacer(Modifier.height(8.dp))
        }
    }
}
