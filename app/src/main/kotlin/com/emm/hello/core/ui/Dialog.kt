package com.emm.hello.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.emm.hello.core.theme.HelloTheme

/**
 * Standard Dialog inspired by shadcn/ui `<AlertDialog />`.
 *
 * Usage: "Review session finished" in StudyScreen.
 *
 * @param title         Dialog title
 * @param description   Descriptive body (optional)
 * @param icon          Optional icon above the title
 * @param confirmText   Confirmation button text
 * @param cancelText    Cancellation button text (null = hides the button)
 * @param onConfirm     Action on confirmation
 * @param onDismiss     Action on cancel or touching outside
 * @param isDangerous   If true, the confirmation button uses destructive color
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

// ─── Previews ────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun HAlertDialogPreview() {
    HelloTheme {
        Surface {
            var show by remember { mutableStateOf(true) }
            if (show) {
                HAlertDialog(
                    title = "Sesión completada",
                    description = "¡Bien hecho! Has repasado todas las tarjetas de esta sesión.",
                    icon = Icons.Outlined.Check,
                    confirmText = "Volver",
                    cancelText = null,
                    onConfirm = { show = false },
                    onDismiss = { show = false },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HAlertDialogDangerousPreview() {
    HelloTheme {
        Surface {
            var show by remember { mutableStateOf(true) }
            if (show) {
                HAlertDialog(
                    title = "Eliminar mazo",
                    description = "Esta acción no se puede deshacer. Perderás todas las tarjetas asociadas.",
                    icon = Icons.Outlined.Delete,
                    confirmText = "Eliminar",
                    cancelText = "Cancelar",
                    isDangerous = true,
                    onConfirm = { show = false },
                    onDismiss = { show = false },
                )
            }
        }
    }
}
