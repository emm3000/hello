package com.emm.hello.core.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

/**
 * Dialog estándar inspirado en shadcn/ui `<AlertDialog />`.
 *
 * Uso: "Sesión de repaso finalizada" en StudyScreen.
 *
 * @param title         Título del diálogo
 * @param description   Cuerpo descriptivo (opcional)
 * @param icon          Icono opcional encima del título
 * @param confirmText   Texto del botón de confirmación
 * @param cancelText    Texto del botón de cancelación (null = oculta el botón)
 * @param onConfirm     Acción al confirmar
 * @param onDismiss     Acción al cancelar o tocar fuera
 * @param isDangerous   Si es true, el botón de confirmación usa color destructive
 */
@Composable
fun HAlertDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    confirmText: String = "Aceptar",
    cancelText: String? = "Cancelar",
    isDangerous: Boolean = false,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        },
        text = description?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            HButton(
                text = confirmText,
                onClick = onConfirm,
                variant = if (isDangerous) ButtonVariant.Destructive else ButtonVariant.Default,
            )
        },
        dismissButton = cancelText?.let {
            {
                HButton(
                    text = it,
                    onClick = onDismiss,
                    variant = ButtonVariant.Ghost,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
