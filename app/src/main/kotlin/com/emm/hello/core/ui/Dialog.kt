package com.emm.hello.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.surface

private val dialogShape = RoundedCornerShape(16.dp)

@Composable
fun HAlertDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    confirmText: String = "Accept",
    cancelText: String? = "Cancel",
    isDangerous: Boolean = false,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        shape = dialogShape,
        containerColor = surface,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
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
                variant = if (isDangerous) HButtonVariant.Text else HButtonVariant.Primary,
                danger = isDangerous,
            )
        },
        dismissButton = cancelText?.let {
            {
                HButton(
                    text = it,
                    onClick = onDismiss,
                    variant = HButtonVariant.Text,
                )
            }
        },
    )
}

@Preview
@Composable
private fun HAlertDialogPreview() {
    HelloTheme {
        Surface {
            var show by remember { mutableStateOf(true) }
            if (show) {
                HAlertDialog(
                    title = "Sesión completada",
                    description = "¡Bien hecho! Repasaste todas las tarjetas de esta sesión.",
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

@Preview
@Composable
private fun HAlertDialogDangerousPreview() {
    HelloTheme {
        Surface {
            var show by remember { mutableStateOf(true) }
            if (show) {
                HAlertDialog(
                    title = "Borrar mazo",
                    description = "Esta acción no se puede deshacer. Perderás todas las tarjetas asociadas.",
                    icon = Icons.Outlined.Delete,
                    confirmText = "Borrar",
                    cancelText = "Cancel",
                    isDangerous = true,
                    onConfirm = { show = false },
                    onDismiss = { show = false },
                )
            }
        }
    }
}
