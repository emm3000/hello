package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.semanticColors
import com.emm.hello.core.theme.spacing

enum class AlertVariant { Default, Destructive, Warning, Success }

/**
 * Alert / callout inspired by shadcn/ui.
 *
 * Usage: error messages in the new card flow ([AlertVariant.Destructive]),
 *        general information, action confirmations.
 */
@Composable
fun HAlert(
    title: String,
    modifier: Modifier = Modifier,
    variant: AlertVariant = AlertVariant.Default,
    description: String? = null,
    icon: ImageVector? = null,
) {
    val (bg, contentColor, iconColor) = alertTokens(variant)
    val stateDescription = alertStateDescription(variant)

    val animBg by animateColorAsState(targetValue = bg, label = "alert_bg")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (stateDescription != null) {
                    this.stateDescription = stateDescription
                }
            },
        shape = MaterialTheme.helloShapes.container,
        color = animBg,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.lg,
                vertical = MaterialTheme.spacing.md,
            ),
            verticalAlignment = Alignment.Top,
        ) {
            val resolvedIcon = icon ?: when (variant) {
                AlertVariant.Destructive -> Icons.Default.Warning
                AlertVariant.Warning -> Icons.Default.Warning
                else -> Icons.Default.Info
            }
            Icon(
                imageVector = resolvedIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 1.dp),
            )
            Spacer(Modifier.width(MaterialTheme.spacing.md))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor,
                )
                if (description != null) {
                    Spacer(Modifier.height(MaterialTheme.spacing.xs))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.metadata,
                        color = contentColor.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun alertTokens(variant: AlertVariant): Triple<Color, Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (variant) {
        AlertVariant.Default -> Triple(
            cs.surfaceContainerHigh,
            cs.onSurface,
            cs.onSurfaceVariant,
        )
        AlertVariant.Destructive -> Triple(
            cs.errorContainer,
            cs.onErrorContainer,
            cs.error,
        )
        AlertVariant.Warning -> Triple(
            MaterialTheme.semanticColors.warning.container,
            MaterialTheme.semanticColors.warning.content,
            MaterialTheme.semanticColors.warning.accent,
        )
        AlertVariant.Success -> Triple(
            cs.tertiaryContainer,
            cs.onTertiaryContainer,
            cs.tertiary,
        )
    }
}

@Composable
private fun alertStateDescription(variant: AlertVariant): String? {
    return when (variant) {
        AlertVariant.Default -> null
        AlertVariant.Destructive -> stringResource(R.string.error_label)
        AlertVariant.Warning -> stringResource(R.string.warning_label)
        AlertVariant.Success -> stringResource(R.string.success_label)
    }
}

@PreviewLightDark
@Composable
private fun HAlertVariantsPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                HAlert(
                    title = "Información",
                    description = "Revisa los datos antes de continuar.",
                    variant = AlertVariant.Default,
                )
                HAlert(
                    title = "Error al generar",
                    description = "No se pudo conectar con el servidor. Verifica tu conexión.",
                    variant = AlertVariant.Destructive,
                )
                HAlert(
                    title = "Atención",
                    description = "Esta acción no se puede deshacer.",
                    variant = AlertVariant.Warning,
                )
                HAlert(
                    title = "Tarjeta guardada",
                    description = "La flashcard fue creada y añadida a tu mazo.",
                    variant = AlertVariant.Success,
                )
            }
        }
    }
}
