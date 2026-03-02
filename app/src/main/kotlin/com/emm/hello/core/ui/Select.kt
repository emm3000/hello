package com.emm.hello.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

/**
 * Dropdown selector inspirado en shadcn/ui `<Select />`.
 *
 * Usa label externo como [HInput], con el mismo visual language:
 * - Borde 1dp outlineVariant → outline animado al focus/open
 * - Fondo transparente, altura mínima 40dp
 * - Chevron rotado cuando está expandido
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
    val cs = MaterialTheme.colorScheme

    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) cs.outline else cs.outlineVariant,
        animationSpec = tween(150),
        label = "select_border",
    )

    Column(modifier = modifier) {
        // ── Label ─────────────────────────────────────────────────────────────
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = cs.onSurface,
        )
        Spacer(Modifier.height(6.dp))

        // ── Dropdown ──────────────────────────────────────────────────────────
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { if (enabled) setIsExpanded(it) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Custom trigger row — same visual as HInput
            Box(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(1.dp, borderColor, MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val displayText = itemSelected?.let { itemLabel(it) }
                    Text(
                        text = displayText ?: placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (displayText != null) {
                            if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.38f)
                        } else {
                            cs.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isExpanded) 180f else 0f),
                        tint = cs.onSurfaceVariant,
                    )
                }
            }

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
                                color = if (itemSelected == option) cs.primary else cs.onSurface,
                                fontWeight = if (itemSelected == option) FontWeight.SemiBold else FontWeight.Normal,
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

// ─── Previews ────────────────────────────────────────────────────────────────

private val demoItems = listOf("Vocabulario B2", "Phrasal Verbs", "Idioms", "Gramática")

@PreviewLightDark
@Composable
private fun HSelectEmptyPreview() {
    HelloTheme {
        Surface {
            HSelect(
                items = demoItems,
                itemSelected = null,
                onItemSelected = {},
                label = "Mazo",
                placeholder = "Seleccionar mazo…",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HSelectWithValuePreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                var selected1 by remember { mutableStateOf<String?>(demoItems.first()) }
                HSelect(
                    items = demoItems,
                    itemSelected = selected1,
                    onItemSelected = { selected1 = it },
                    label = "Mazo seleccionado",
                )

                var selected2 by remember { mutableStateOf<String?>(null) }
                HSelect(
                    items = demoItems,
                    itemSelected = selected2,
                    onItemSelected = { selected2 = it },
                    label = "Dificultad",
                    enabled = false,
                    placeholder = "Desactivado",
                )
            }
        }
    }
}
