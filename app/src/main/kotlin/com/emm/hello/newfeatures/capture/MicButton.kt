package com.emm.hello.newfeatures.capture

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.ui.HIconButton

private val micButtonSize: Dp = 44.dp
private val ringStroke: Dp = 2.dp
private const val RING_GROWTH = 0.45f

@Composable
fun MicButton(
    isListening: Boolean,
    level: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HIconButton(
        icon = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
        contentDescription = stringResource(R.string.capture_mic_content_description),
        onClick = onClick,
        modifier = modifier
            .size(micButtonSize)
            .drawBehind {
                val amplitude: Float = level()
                if (amplitude <= 0f) return@drawBehind

                drawCircle(
                    color = inkFaint,
                    radius = size.minDimension / 2f * (1f + amplitude * RING_GROWTH),
                    style = Stroke(width = ringStroke.toPx()),
                )
            },
        tint = ink,
        buttonSize = micButtonSize,
    )
}

@PreviewLightDark
@Composable
private fun MicButtonPreview() {
    HelloTheme {
        MicButton(isListening = true, level = { 0.7f }, onClick = {})
    }
}
