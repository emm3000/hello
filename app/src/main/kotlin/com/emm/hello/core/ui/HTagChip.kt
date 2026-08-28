package com.emm.hello.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.onInk
import com.emm.hello.core.theme.pageBackground
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.theme.surfaceRaised

@Composable
fun HChip(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val containerColor: Color = if (active) ink else surfaceRaised
    val contentColor: Color = if (active) onInk else ink

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.helloShapes.pill)
            .background(containerColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
        if (onRemove != null) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Quitar $label",
                tint = contentColor,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRemove),
            )
        }
    }
}

@Composable
fun HTagChip(
    tag: String,
    modifier: Modifier = Modifier,
    removable: Boolean = false,
    onRemove: () -> Unit = {},
) {
    HChip(
        label = tag,
        modifier = modifier,
        onRemove = onRemove.takeIf { removable },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HChipStatesPreview() {
    HelloTheme {
        Surface(color = pageBackground) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HChip(label = "inglés")
                HChip(label = "activo", active = true)
                HChip(label = "quitar", onRemove = {})
                HChip(label = "activo", active = true, onRemove = {})
            }
        }
    }
}
