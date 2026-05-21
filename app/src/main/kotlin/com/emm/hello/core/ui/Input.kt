package com.emm.hello.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
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
    val borderColor = fieldShellBorderColor(
        isError = isError,
        enabled = enabled,
        isActive = isFocused,
    )

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
            modifier = fieldShellContainerModifier(borderColor = borderColor)
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
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = fieldShellContentColor(enabled)),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                InputDecoration(
                    value = value,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    singleLine = singleLine,
                    placeholderColor = fieldShellPlaceholderColor(enabled),
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
    placeholderColor: androidx.compose.ui.graphics.Color,
    innerTextField: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(fieldShellContentPadding()),
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
                    style = MaterialTheme.typography.bodyMedium,
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

@PreviewLightDark
@Composable
private fun HInputPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
            ) {
                var name by remember { mutableStateOf("") }
                HInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Deck name",
                    placeholder = "E.g.: English B2 Vocabulary",
                )

                var word by remember { mutableStateOf("serendipity") }
                HInput(
                    value = word,
                    onValueChange = { word = it },
                    label = "Word",
                    supportingText = "Type the word in English",
                )

                var broken by remember { mutableStateOf("") }
                HInput(
                    value = broken,
                    onValueChange = { broken = it },
                    label = "Required field",
                    placeholder = "This field is required",
                    errorMessage = "This field is required",
                )

                var notes by remember { mutableStateOf("") }
                HInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes (multiline)",
                    singleLine = false,
                    minLines = 4,
                    placeholder = "Write your notes here…",
                )

                HInput(
                    value = "Disabled field",
                    onValueChange = {},
                    label = "Disabled",
                    enabled = false,
                )

                HInput(
                    value = "Read only",
                    onValueChange = {},
                    label = "Accessible state",
                    supportingText = "Field compatible with screen readers",
                    readOnly = true,
                )
            }
        }
    }
}
