package com.emm.hello.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

/**
 * Minimal indeterminate spinner inspired by shadcn/ui.
 *
 * @param size explicit size; defaults to 48dp for standalone loading states.
 *   Pass a smaller value (e.g. 18dp) for inline / row-level spinners.
 */
@Composable
fun HLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 3.dp,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = strokeWidth,
    )
}

@PreviewLightDark
@Composable
private fun HLoadingSpinnerPreview() {
    HelloTheme {
        HLoadingSpinner()
    }
}
