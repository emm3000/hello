package com.emm.hello.newfeatures.card

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

/**
 * Campo de solo lectura clickeable, con la misma apariencia que [com.emm.hello.core.ui.HInput].
 * Usado para mostrar la categoría seleccionada y abrir el BottomSheet de selección.
 */
@Composable
fun JustClickableInput(
    value: String,
    label: String,
    onClick: () -> Unit,
    placeholder: String = "Seleccionar categoría",
) {
    val cs = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = cs.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp)
                .clip(MaterialTheme.shapes.small)
                .border(1.dp, cs.outlineVariant, MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val display = value.ifBlank { null }
                Text(
                    text = display ?: placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (display != null) {
                        cs.onSurface
                    } else {
                        cs.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = cs.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JustClickableInputPreview() {
    HelloTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column {
                JustClickableInput(
                    value = "",
                    label = "Category",
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(16.dp))
                JustClickableInput(
                    value = "Idioms",
                    label = "Category",
                    onClick = {}
                )
            }
        }
    }
}
