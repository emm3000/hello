package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

// ─── Variantes ──────────────────────────────────────────────────────────────

enum class AlertVariant { Default, Destructive, Warning, Success }

/**
 * Alert / callout inspirado en shadcn/ui.
 *
 * Uso: mensajes de error en NewCardScreen ([AlertVariant.Destructive]),
 *      información general, confirmaciones de acción.
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

    val animBg by animateColorAsState(targetValue = bg, label = "alert_bg")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(animBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
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
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor,
                )
                if (description != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
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
            com.emm.hello.core.theme.shadcnWarningContainer,
            com.emm.hello.core.theme.shadcnOnWarningContainer,
            com.emm.hello.core.theme.shadcnWarning,
        )
        AlertVariant.Success -> Triple(
            cs.tertiaryContainer,
            cs.onTertiaryContainer,
            cs.tertiary,
        )
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun HAlertVariantsPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
