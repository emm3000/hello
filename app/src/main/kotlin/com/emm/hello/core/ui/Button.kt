package com.emm.hello.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

// ─── Variantes ──────────────────────────────────────────────────────────────

enum class ButtonVariant { Default, Destructive, Outline, Secondary, Ghost, Link }

/**
 * Botón principal inspirado en shadcn/ui.
 *
 * Soporta variantes: Default, Destructive, Outline, Secondary, Ghost, Link.
 * Tiene soporte para estado de carga con [isLoading].
 */
@Composable
fun HButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = when (variant) {
        ButtonVariant.Default -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
        )
        ButtonVariant.Destructive -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.38f),
        )
        ButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ButtonVariant.Outline, ButtonVariant.Ghost, ButtonVariant.Link ->
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
    }

    val border: BorderStroke? = when (variant) {
        ButtonVariant.Outline -> BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )
        else -> null
    }

    when (variant) {
        ButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 40.dp),
            enabled = enabled && !isLoading,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            contentPadding = contentPadding,
        ) {
            ButtonContent(leadingIcon = leadingIcon, isLoading = isLoading, content = content)
        }
        ButtonVariant.Link -> TextButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 40.dp),
            enabled = enabled && !isLoading,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        ) {
            ButtonContent(leadingIcon = leadingIcon, isLoading = isLoading, content = content)
        }
        ButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 40.dp),
            enabled = enabled && !isLoading,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            border = border,
            contentPadding = contentPadding,
        ) {
            ButtonContent(leadingIcon = leadingIcon, isLoading = isLoading, content = content)
        }
        else -> Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 40.dp),
            enabled = enabled && !isLoading,
            colors = colors,
            contentPadding = contentPadding,
        ) {
            ButtonContent(leadingIcon = leadingIcon, isLoading = isLoading, content = content)
        }
    }
}

@Composable
private fun RowScope.ButtonContent(
    leadingIcon: ImageVector?,
    isLoading: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = LocalContentColor.current,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(8.dp))
    } else if (leadingIcon != null) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
    }
    content()
}

// ─── Convenience overloads ───────────────────────────────────────────────────

@Composable
fun HButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    HButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        enabled = enabled,
        isLoading = isLoading,
        leadingIcon = leadingIcon,
    ) {
        Text(
            text = if (isLoading) "Cargando…" else text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun HButtonVariantsPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ButtonVariant.entries.forEach { variant ->
                    HButton(
                        text = variant.name,
                        onClick = {},
                        variant = variant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HButtonWithIconPreview() {
    HelloTheme {
        Surface(Modifier.padding(16.dp)) {
            HButton(
                text = "Nueva tarjeta",
                onClick = {},
                leadingIcon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HButtonLoadingPreview() {
    HelloTheme {
        Surface(Modifier.padding(16.dp)) {
            HButton(
                text = "Generar",
                onClick = {},
                isLoading = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
