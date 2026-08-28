package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentDivider
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing

internal const val FIELD_SHELL_BORDER_ANIMATION_MS = 150
internal const val FIELD_SHELL_DISABLED_ALPHA = 0.38f
internal const val FIELD_SHELL_PLACEHOLDER_ALPHA = 0.6f
internal val fieldShellMinHeight = 48.dp

@Composable
internal fun FieldShell(
    modifier: Modifier = Modifier,
    label: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    monoLabel: Boolean = false,
    content: @Composable () -> Unit,
) {
    val helperText = errorMessage ?: supportingText
    val isError = errorMessage != null

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        if (label != null) {
            if (monoLabel) {
                Text(
                    text = label.uppercase(),
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.12.em,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        instrumentMuted
                    },
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = fieldShellLabelColor(isError = isError, enabled = enabled),
                )
            }
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

@Composable
internal fun fieldShellContainerModifier(
    modifier: Modifier = Modifier,
    borderColor: Color,
): Modifier = modifier
    .fillMaxWidth()
    .defaultMinSize(minHeight = fieldShellMinHeight)
    .clip(MaterialTheme.helloShapes.control)
    .border(
        width = 1.dp,
        color = borderColor,
        shape = MaterialTheme.helloShapes.control,
    )

@Composable
internal fun fieldShellBorderColor(
    isError: Boolean,
    enabled: Boolean,
    isActive: Boolean,
): Color {
    val targetColor = when {
        isError -> MaterialTheme.colorScheme.error
        isActive -> instrumentAccent
        else -> instrumentDivider
    }.let { color ->
        if (enabled) color else color.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = FIELD_SHELL_BORDER_ANIMATION_MS),
        label = "field_shell_border",
    )

    return animatedColor
}

@Composable
internal fun fieldShellContentColor(enabled: Boolean): Color {
    val colorScheme = MaterialTheme.colorScheme
    return if (enabled) {
        colorScheme.onSurface
    } else {
        colorScheme.onSurface.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
    }
}

@Composable
internal fun fieldShellPlaceholderColor(enabled: Boolean): Color {
    val baseColor = instrumentMuted
    return if (enabled) baseColor else baseColor.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
}

@Composable
internal fun fieldShellLabelColor(isError: Boolean, enabled: Boolean): Color {
    val colorScheme = MaterialTheme.colorScheme
    val baseColor = if (isError) colorScheme.error else colorScheme.onSurface
    return if (enabled) baseColor else colorScheme.onSurface.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
}

@Composable
internal fun fieldShellSupportingColor(isError: Boolean, enabled: Boolean): Color {
    val colorScheme = MaterialTheme.colorScheme
    val baseColor = if (isError) colorScheme.error else colorScheme.onSurfaceVariant
    return if (enabled) baseColor else baseColor.copy(alpha = FIELD_SHELL_DISABLED_ALPHA)
}

@Composable
internal fun fieldShellContentPadding(): PaddingValues = PaddingValues(
    horizontal = MaterialTheme.spacing.md,
    vertical = MaterialTheme.spacing.sm,
)
