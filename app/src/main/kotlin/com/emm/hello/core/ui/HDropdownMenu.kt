package com.emm.hello.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberElev
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberPrimary

/**
 * Single entry of an [HDropdownMenu]. Destructive entries render their label
 * with the accent tint to signal a consequential action (e.g. delete).
 */
@Immutable
data class HMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false,
)

/**
 * Overflow / context dropdown menu retokened to Ember, mirroring [HSelect]'s
 * popup styling (emberElev container, bodyMedium item text, emberMuted icons).
 *
 * The caller owns the [expanded] state and supplies the [items]; selecting an
 * item invokes its [HMenuItem.onClick]. Dismissal does not auto-fire callbacks,
 * so callers should close the menu inside each [HMenuItem.onClick] as needed.
 */
@Composable
fun HDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<HMenuItem>,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = emberElev,
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.isDestructive) emberAccent else emberPrimary,
                    )
                },
                onClick = item.onClick,
                leadingIcon = item.icon?.let { icon ->
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = emberMuted,
                        )
                    }
                },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HDropdownMenuPreview() {
    HelloTheme {
        HDropdownMenu(
            expanded = true,
            onDismissRequest = {},
            items = listOf(
                HMenuItem(
                    label = "Editar",
                    onClick = {},
                    icon = Icons.Outlined.Edit,
                ),
                HMenuItem(
                    label = "Eliminar",
                    onClick = {},
                    icon = Icons.Outlined.Delete,
                    isDestructive = true,
                ),
            ),
        )
    }
}
