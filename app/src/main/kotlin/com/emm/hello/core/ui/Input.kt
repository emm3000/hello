package com.emm.hello.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

/**
 * Input de texto inspirado en shadcn/ui.
 *
 * Envuelve [OutlinedTextField] de Material 3 añadiendo:
 * - [label] como Text sobre el campo (estilo shadcn)
 * - [supportingText] debajo en color muted
 * - [errorMessage] que reemplaza el supportingText y aplica colores de error
 *
 * Uso: NewDeckScreen (nombre/descripción), NewCardScreen (word),
 *      FlashcardDetailScreen (edición futura).
 */
@Composable
fun HInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
    ),
) {
    val isError = errorMessage != null

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            placeholder = placeholder?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            shape = shape,
            colors = colors,
        )

        val helperText = errorMessage ?: supportingText
        if (helperText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun HInputPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                var name by remember { mutableStateOf("") }
                HInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre del mazo",
                    placeholder = "Ej: Vocabulario de Inglés B2",
                )

                var word by remember { mutableStateOf("serendipity") }
                HInput(
                    value = word,
                    onValueChange = { word = it },
                    label = "Palabra",
                    supportingText = "Escribe la palabra en inglés",
                )

                var broken by remember { mutableStateOf("") }
                HInput(
                    value = broken,
                    onValueChange = { broken = it },
                    label = "Campo requerido",
                    placeholder = "Este campo es obligatorio",
                    errorMessage = if (broken.isBlank()) "Este campo es obligatorio" else null,
                )

                var notes by remember { mutableStateOf("") }
                HInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notas (multiline)",
                    singleLine = false,
                    minLines = 4,
                )
            }
        }
    }
}
