// ⚠️ DEPRECATED — usa com.emm.hello.core.ui.HSelect en su lugar.
// Este archivo se mantiene temporalmente para evitar rompimiento de compilación.
// Será eliminado en la siguiente limpieza.

package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.hello.core.theme.HelloTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GemaDropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textLabel: String,
    items: List<T>,
    itemSelected: T?,
    onItemSelected: (T) -> Unit,
) {

    val (isExpanded, setIsExpanded) = remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = setIsExpanded
    ) {
        OutlinedTextField(
            value = itemSelected?.toString().orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            placeholder = {
                Text(textLabel)
            },
            label = {
                Text(textLabel)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { setIsExpanded(false) }
        ) {
            items.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option?.toString().orEmpty()) },
                    onClick = {
                        onItemSelected(option)
                        setIsExpanded(false)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun GemaDropdownPreview() {
    HelloTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            GemaDropdown(
                modifier = Modifier.fillMaxWidth(),
                textLabel = "Grado",
                items = listOf("1ro", "2do", "3ro", "4to", "5to"),
                itemSelected = null,
                onItemSelected = {}
            )
        }
    }
}