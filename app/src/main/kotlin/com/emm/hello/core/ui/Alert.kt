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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberBad
import com.emm.hello.core.theme.emberBadSoft
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberElev
import com.emm.hello.core.theme.emberGood
import com.emm.hello.core.theme.emberGoodSoft
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberWarn
import com.emm.hello.core.theme.emberWarnSoft
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing

enum class AlertVariant { Default, Destructive, Warning, Success }

private val alertShape = RoundedCornerShape(12.dp)

/**
 * Ember-styled alert. Retokened to use Ember soft-bg tones.
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
    val stateDesc = alertStateDescription(variant)

    val animBg by animateColorAsState(targetValue = bg, label = "alert_bg")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (stateDesc != null) {
                    this.stateDescription = stateDesc
                }
            },
        shape = alertShape,
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
    return when (variant) {
        AlertVariant.Default -> Triple(emberElev, emberOnBg, emberOnBg.copy(alpha = 0.6f))
        AlertVariant.Destructive -> Triple(emberBadSoft, emberBad, emberBad)
        AlertVariant.Warning -> Triple(emberWarnSoft, emberWarn, emberWarn)
        AlertVariant.Success -> Triple(emberGoodSoft, emberGood, emberGood)
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

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HAlertVariantsPreview() {
    HelloTheme {
        Surface(color = emberBg) {
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
                    description = "No se pudo conectar. Verifica tu conexión.",
                    variant = AlertVariant.Destructive,
                )
                HAlert(
                    title = "Aviso",
                    description = "Esta acción no se puede deshacer.",
                    variant = AlertVariant.Warning,
                )
                HAlert(
                    title = "Tarjeta guardada",
                    description = "La tarjeta fue creada y añadida a tu mazo.",
                    variant = AlertVariant.Success,
                )
            }
        }
    }
}
