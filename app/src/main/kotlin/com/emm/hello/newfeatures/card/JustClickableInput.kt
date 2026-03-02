package com.emm.hello.newfeatures.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Campo de texto de solo lectura que actúa como un trigger clickeable.
 * Usa el mismo visual language que [com.emm.hello.core.ui.HInput]:
 * label externo en labelMedium + OutlinedTextField.
 */
@Composable
fun JustClickableInput(
    value: String,
    label: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            placeholder = {
                Text(
                    text = "Seleccionar categoría",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            shape = MaterialTheme.shapes.small,
        )
    }
}