package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dropdown selector inspirado en shadcn/ui `<Select />`.
 *
 * Usa label externo como [HInput], con el mismo visual language.
 * Uso: selector de Deck en NewCard, selector de Dificultad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> HSelect(
    items: List<T>,
    itemSelected: T?,
    onItemSelected: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemLabel: (T) -> String = { it.toString() },
    placeholder: String = "Seleccionar…",
) {
    val (isExpanded, setIsExpanded) = remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { if (enabled) setIsExpanded(it) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = itemSelected?.let { itemLabel(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                shape = MaterialTheme.shapes.small,
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { setIsExpanded(false) },
            ) {
                items.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = itemLabel(option),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onItemSelected(option)
                            setIsExpanded(false)
                        },
                    )
                }
            }
        }
    }
}
