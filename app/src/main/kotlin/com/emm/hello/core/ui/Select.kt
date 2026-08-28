package com.emm.hello.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.surface
import com.emm.hello.core.theme.spacing

private const val COLLAPSED_ARROW_ROTATION_DEGREES = 180f

@Composable
fun HSelectTrigger(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    errorMessage: String? = null,
    placeholder: String = "Select…",
) {
    val displayText = value.ifBlank { null }
    val borderColor = fieldShellBorderColor(
        isError = errorMessage != null,
        enabled = enabled,
        isActive = false,
    )

    FieldShell(
        modifier = modifier,
        label = label,
        supportingText = supportingText,
        errorMessage = errorMessage,
        enabled = enabled,
    ) {
        Box(
            modifier = fieldShellContainerModifier(borderColor = borderColor)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                    if (!enabled) {
                        disabled()
                    }
                    if (errorMessage != null) {
                        error(errorMessage)
                    }
                }
                .let {
                    if (enabled) {
                        it.then(Modifier.clickable(onClick = onClick))
                    } else {
                        it
                    }
                }
                .padding(fieldShellContentPadding()),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = displayText ?: placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (displayText != null) {
                        fieldShellContentColor(enabled)
                    } else {
                        fieldShellPlaceholderColor(enabled)
                    },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = fieldShellSupportingColor(isError = false, enabled = enabled),
                )
            }
        }
    }
}

@Composable
fun <T> HSelect(
    items: List<T>,
    itemSelected: T?,
    onItemSelected: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    errorMessage: String? = null,
    itemLabel: (T) -> String = { it.toString() },
    placeholder: String = "Select…",
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayText = itemSelected?.let { itemLabel(it) }
    val borderColor = fieldShellBorderColor(
        isError = errorMessage != null,
        enabled = enabled,
        isActive = isExpanded,
    )

    FieldShell(
        modifier = modifier,
        label = label,
        supportingText = supportingText,
        errorMessage = errorMessage,
        enabled = enabled,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = fieldShellContainerModifier(borderColor = borderColor)
                    .semantics {
                        contentDescription = label
                        role = Role.Button
                        if (!enabled) {
                            disabled()
                        }
                        if (errorMessage != null) {
                            error(errorMessage)
                        }
                    }
                    .let {
                        if (enabled) {
                            it.then(Modifier.clickable { isExpanded = true })
                        } else {
                            it
                        }
                    }
                    .padding(fieldShellContentPadding()),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = displayText ?: placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (displayText != null) {
                            fieldShellContentColor(enabled)
                        } else {
                            fieldShellPlaceholderColor(enabled)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isExpanded) COLLAPSED_ARROW_ROTATION_DEGREES else 0f),
                        tint = fieldShellSupportingColor(isError = false, enabled = enabled),
                    )
                }
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                containerColor = surface,
            ) {
                items.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = itemLabel(option),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (itemSelected == option) {
                                    ink
                                } else {
                                    ink
                                },
                                fontWeight = if (itemSelected == option) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onItemSelected(option)
                            isExpanded = false
                        },
                    )
                }
            }
        }
    }
}

private val demoItems = listOf("Vocabulario B2", "Verbos frasales", "Modismos", "Gramática")

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HSelectEmptyPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            HSelect(
                items = demoItems,
                itemSelected = null,
                onItemSelected = {},
                label = "Mazo",
                placeholder = "Selecciona un mazo…",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HSelectWithValuePreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
            ) {
                var selected by remember { mutableStateOf<String?>(demoItems.first()) }
                HSelect(
                    items = demoItems,
                    itemSelected = selected,
                    onItemSelected = { selected = it },
                    label = "Mazo seleccionado",
                )

                HSelect(
                    items = demoItems,
                    itemSelected = null,
                    onItemSelected = {},
                    label = "Dificultad",
                    enabled = false,
                    placeholder = "Desactivado",
                )
            }
        }
    }
}
