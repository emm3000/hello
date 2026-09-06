package com.emm.hello.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.spacing

@Composable
fun HInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    variant: HFieldVariant = HFieldVariant.Outlined,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    rows: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isError: Boolean = errorMessage != null
    val isFocused by interactionSource.collectIsFocusedAsState()

    val effectiveSingleLine = singleLine && rows <= 1
    val effectiveMinLines = if (rows > 1) rows else minLines
    val effectiveMaxLines = if (rows > 1) rows else maxLines

    val containerModifier: Modifier = when (variant) {
        HFieldVariant.Outlined -> {
            val borderColor: Color = fieldShellBorderColor(isError = isError, enabled = enabled, isActive = isFocused)
            Modifier.fieldShellContainer(borderColor = borderColor, shape = MaterialTheme.helloShapes.control)
        }
        HFieldVariant.Underline -> {
            val lineColor: Color = fieldShellUnderlineColor(isError = isError, enabled = enabled, isActive = isFocused)
            Modifier.fieldShellUnderline(lineColor = lineColor)
        }
    }

    val contentPadding: PaddingValues = when (variant) {
        HFieldVariant.Outlined -> fieldShellContentPadding()
        HFieldVariant.Underline -> PaddingValues(horizontal = 0.dp, vertical = MaterialTheme.spacing.sm)
    }

    val baseTextStyle: TextStyle = when (variant) {
        HFieldVariant.Outlined -> MaterialTheme.typography.bodyMedium
        HFieldVariant.Underline -> MaterialTheme.typography.displaySmall
    }

    val placeholderColor: Color = when (variant) {
        HFieldVariant.Outlined -> fieldShellPlaceholderColor(enabled)
        HFieldVariant.Underline -> inkFaint
    }

    FieldShell(
        modifier = modifier,
        label = label,
        supportingText = supportingText,
        errorMessage = errorMessage,
        enabled = enabled,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = containerModifier
                .semantics {
                    if (label != null) {
                        contentDescription = label
                    }
                    if (isError) {
                        error(errorMessage)
                    }
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = effectiveSingleLine,
            minLines = effectiveMinLines,
            maxLines = effectiveMaxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            textStyle = baseTextStyle.copy(color = fieldShellContentColor(enabled)),
            cursorBrush = SolidColor(ink),
            decorationBox = { innerTextField ->
                InputDecoration(
                    value = value,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    singleLine = effectiveSingleLine,
                    placeholderStyle = baseTextStyle,
                    placeholderColor = placeholderColor,
                    contentPadding = contentPadding,
                    innerTextField = innerTextField,
                )
            },
        )
    }
}

@Composable
private fun InputDecoration(
    value: String,
    placeholder: String?,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    singleLine: Boolean,
    placeholderStyle: TextStyle,
    placeholderColor: Color,
    contentPadding: PaddingValues,
    innerTextField: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (leadingIcon != null) {
            Box { leadingIcon() }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (value.isEmpty() && placeholder != null) {
                Text(
                    text = placeholder,
                    style = placeholderStyle,
                    color = placeholderColor,
                )
            }
            innerTextField()
        }

        if (trailingIcon != null) {
            Box(
                modifier = Modifier.align(
                    if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ),
            ) {
                trailingIcon()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HInputOutlinedEmptyPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Box(modifier = Modifier.padding(16.dp)) {
                var name by remember { mutableStateOf("") }
                HInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre del mazo",
                    placeholder = "Ej: Vocabulario inglés B2",
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HInputOutlinedWithValuePreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Box(modifier = Modifier.padding(16.dp)) {
                var word by remember { mutableStateOf("serendipity") }
                HInput(
                    value = word,
                    onValueChange = { word = it },
                    label = "Palabra",
                    supportingText = "Escribe la palabra en inglés",
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HInputOutlinedErrorPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Box(modifier = Modifier.padding(16.dp)) {
                var broken by remember { mutableStateOf("") }
                HInput(
                    value = broken,
                    onValueChange = { broken = it },
                    label = "Campo obligatorio",
                    placeholder = "Este campo es obligatorio",
                    errorMessage = "Este campo es obligatorio",
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HInputUnderlineEmptyPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Box(modifier = Modifier.padding(16.dp)) {
                var expression by remember { mutableStateOf("") }
                HInput(
                    value = expression,
                    onValueChange = { expression = it },
                    placeholder = "Escribe una palabra o frase",
                    variant = HFieldVariant.Underline,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HInputUnderlineWithValuePreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Box(modifier = Modifier.padding(16.dp)) {
                var expression by remember { mutableStateOf("serendipity") }
                HInput(
                    value = expression,
                    onValueChange = { expression = it },
                    variant = HFieldVariant.Underline,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HInputUnderlineErrorPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Box(modifier = Modifier.padding(16.dp)) {
                var expression by remember { mutableStateOf("") }
                HInput(
                    value = expression,
                    onValueChange = { expression = it },
                    placeholder = "Escribe una palabra o frase",
                    errorMessage = "Este campo es obligatorio",
                    variant = HFieldVariant.Underline,
                )
            }
        }
    }
}
