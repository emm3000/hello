package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.destructiveInk
import com.emm.hello.core.theme.hairline
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.outline
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.theme.surface

internal const val FIELD_SHELL_BORDER_ANIMATION_MS = 150
internal const val FIELD_SHELL_DISABLED_ALPHA = 0.38f
internal const val FIELD_SHELL_PLACEHOLDER_ALPHA = 0.6f
internal val fieldShellMinHeight = 48.dp
internal val fieldShellUnderlineMinHeight = 56.dp
internal val fieldShellUnderlineThickness = 2.dp

enum class HFieldVariant { Outlined, Underline }

@Composable
internal fun FieldShell(
    modifier: Modifier = Modifier,
    label: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val helperText: String? = errorMessage ?: supportingText
    val isError: Boolean = errorMessage != null

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.metadata,
                color = if (isError) destructiveInk else inkMuted,
            )
        }

        content()

        if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.metadata,
                color = fieldShellSupportingColor(isError = isError, enabled = enabled),
            )
        }
    }
}

internal fun Modifier.fieldShellContainer(
    borderColor: Color,
    shape: Shape,
): Modifier = this
    .fillMaxWidth()
    .defaultMinSize(minHeight = fieldShellMinHeight)
    .clip(shape)
    .background(surface)
    .border(
        width = 1.dp,
        color = borderColor,
        shape = shape,
    )

internal fun Modifier.fieldShellUnderline(lineColor: Color): Modifier = this
    .fillMaxWidth()
    .defaultMinSize(minHeight = fieldShellUnderlineMinHeight)
    .drawBehind {
        drawLine(
            color = lineColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = fieldShellUnderlineThickness.toPx(),
        )
    }

@Composable
internal fun fieldShellBorderColor(
    isError: Boolean,
    enabled: Boolean,
    isActive: Boolean,
): Color {
    val targetColor: Color = when {
        isError -> destructiveInk
        isActive -> ink
        else -> hairline
    }.let { color ->
        if (enabled) color else color.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
    }

    val animatedColor: Color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = FIELD_SHELL_BORDER_ANIMATION_MS),
        label = "field_shell_border",
    )

    return animatedColor
}

@Composable
internal fun fieldShellUnderlineColor(
    isError: Boolean,
    enabled: Boolean,
    isActive: Boolean,
): Color {
    val targetColor: Color = when {
        isError -> destructiveInk
        isActive -> ink
        else -> outline
    }.let { color ->
        if (enabled) color else color.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
    }

    val animatedColor: Color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = FIELD_SHELL_BORDER_ANIMATION_MS),
        label = "field_shell_underline",
    )

    return animatedColor
}

internal fun fieldShellContentColor(enabled: Boolean): Color = if (enabled) ink else inkFaint

internal fun fieldShellPlaceholderColor(enabled: Boolean): Color {
    val baseColor = inkMuted
    return if (enabled) baseColor else baseColor.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
}

internal fun fieldShellSupportingColor(isError: Boolean, enabled: Boolean): Color {
    val baseColor: Color = if (isError) destructiveInk else inkMuted
    return if (enabled) baseColor else baseColor.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
}

@Composable
internal fun fieldShellContentPadding(): PaddingValues = PaddingValues(
    horizontal = MaterialTheme.spacing.md,
    vertical = MaterialTheme.spacing.sm,
)
