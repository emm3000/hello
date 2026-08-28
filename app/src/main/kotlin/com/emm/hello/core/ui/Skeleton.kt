package com.emm.hello.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

private const val SHIMMER_START_X = -400f
private const val SHIMMER_END_X = 1200f
private const val SHIMMER_WIDTH = 400f
private const val SHIMMER_DURATION_MS = 1200
private const val DECK_ITEM_TITLE_WIDTH = 0.55f
private const val DECK_ITEM_SUBTITLE_WIDTH = 0.35f
private const val PREVIEW_TITLE_WIDTH = 0.5f
private const val PREVIEW_LAST_LINE_WIDTH = 0.7f

/** Specify [modifier] with explicit width/height — the box has no intrinsic size. */
@Composable
fun HSkeleton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface

    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateX by transition.animateFloat(
        initialValue = SHIMMER_START_X,
        targetValue = SHIMMER_END_X,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton_translateX",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateX, 0f),
        end = Offset(translateX + SHIMMER_WIDTH, 0f),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush),
    )
}

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
                    .fillMaxWidth(DECK_ITEM_TITLE_WIDTH)
                    .height(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            HSkeleton(
                modifier = Modifier
                    .fillMaxWidth(DECK_ITEM_SUBTITLE_WIDTH)
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

@Composable
fun TodaySkeleton(count: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
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

@PreviewLightDark
@Composable
private fun HSkeletonPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HSkeleton(Modifier.fillMaxWidth(PREVIEW_TITLE_WIDTH).height(20.dp))
                HSkeleton(Modifier.fillMaxWidth().height(14.dp))
                HSkeleton(Modifier.fillMaxWidth().height(14.dp))
                HSkeleton(Modifier.fillMaxWidth(PREVIEW_LAST_LINE_WIDTH).height(14.dp))
                HSkeleton(Modifier.size(width = 120.dp, height = 36.dp), cornerRadius = 20.dp)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TodaySkeletonPreview() {
    HelloTheme {
        Surface {
            TodaySkeleton(
                count = 4,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TextSkeletonPreview() {
    HelloTheme {
        Surface {
            TextSkeleton(
                lines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}
