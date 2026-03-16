package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

private const val inputBorderAnimationMs = 150
private const val inputDisabledAlpha = 0.38f
private const val inputPlaceholderAlpha = 0.6f
private val inputContentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
private val inputLeadingIconOffset = 36.dp
private val inputLeadingTextStartPadding = 28.dp
private val inputTrailingTextEndPadding = 40.dp

/**
 * Input de texto inspirado en shadcn/ui — look & feel exacto.
 *
 * Características:
 * - Label externo sobre el campo (como shadcn `<Label />` + `<Input />`)
 * - Borde `outlineVariant` en reposo → `outline` (ring) al focus (animado)
 * - Fondo transparente (sin fill)
 * - Sin trailing icon / label flotante de Material 3
 * - Error: borde rojo, helper text rojo abajo
 * - Supporting text en `muted-foreground` cuando no hay error
 * - Altura mínima fija de 40 dp (shadcn h-10)
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isError = errorMessage != null
    val isFocused by interactionSource.collectIsFocusedAsState()

    val cs = MaterialTheme.colorScheme

    // Border color: error → error / focused → outline (ring) / idle → outlineVariant
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> cs.error
            isFocused -> cs.outline
            else -> cs.outlineVariant
        },
        animationSpec = tween(durationMillis = inputBorderAnimationMs),
        label = "input_border",
    )

    // Text color dims when disabled
    val textColor = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = inputDisabledAlpha)

    val boxAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
    val trailingAlignment = if (singleLine) Alignment.CenterEnd else Alignment.TopEnd

    Column(modifier = modifier) {
        // ── External label (shadcn pattern) ──────────────────────────────────
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isError) cs.error else cs.onSurface,
            )
            Spacer(Modifier.height(6.dp))
        }

        // ── Field box ─────────────────────────────────────────────────────────
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp)
                .clip(MaterialTheme.shapes.small)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = MaterialTheme.shapes.small,
                ),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
            cursorBrush = SolidColor(cs.onSurface),
            decorationBox = { innerTextField ->
                InputDecoration(
                    value = value,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    boxAlignment = boxAlignment,
                    trailingAlignment = trailingAlignment,
                    placeholderColor = cs.onSurfaceVariant.copy(alpha = inputPlaceholderAlpha),
                    innerTextField = innerTextField,
                )
            },

        )

        // ── Helper / error text ───────────────────────────────────────────────
        val helperText = errorMessage ?: supportingText
        if (helperText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) cs.error else cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InputDecoration(
    value: String,
    placeholder: String?,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    boxAlignment: Alignment,
    trailingAlignment: Alignment,
    placeholderColor: androidx.compose.ui.graphics.Color,
    innerTextField: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.padding(inputContentPadding),
        contentAlignment = boxAlignment,
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .align(boxAlignment)
                    .padding(end = inputLeadingIconOffset),
            ) { leadingIcon() }
        }

        if (value.isEmpty() && placeholder != null) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = placeholderColor,
                modifier = Modifier
                    .align(boxAlignment)
                    .then(
                        if (leadingIcon != null) {
                            Modifier.padding(start = inputLeadingTextStartPadding)
                        } else {
                            Modifier
                        }
                    ),
            )
        }

        val textEndPadding: Dp = if (trailingIcon != null) inputTrailingTextEndPadding else 0.dp
        Box(
            modifier = Modifier
                .align(boxAlignment)
                .padding(
                    start = if (leadingIcon != null) inputLeadingTextStartPadding else 0.dp,
                    end = textEndPadding,
                )
                .fillMaxWidth(),
        ) { innerTextField() }

        if (trailingIcon != null) {
            Box(Modifier.align(trailingAlignment)) { trailingIcon() }
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
                    errorMessage = "Este campo es obligatorio",
                )

                var notes by remember { mutableStateOf("") }
                HInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notas (multiline)",
                    singleLine = false,
                    minLines = 4,
                    placeholder = "Escribe tus notas aquí…",
                )

                HInput(
                    value = "Campo desactivado",
                    onValueChange = {},
                    label = "Desactivado",
                    enabled = false,
                )
            }
        }
    }
}
