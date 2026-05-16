package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing

@Composable
fun HTagChip(
    tag: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Secondary,
    removable: Boolean = false,
    onRemove: () -> Unit = {},
) {
    val (containerColor, contentColor) = badgeColors(variant)
    val removeLabel = "Remove tag $tag"

    Surface(
        modifier = modifier.semantics {
            if (removable) stateDescription = "Remove tag $tag"
        },
        shape = MaterialTheme.helloShapes.pill,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.md,
                end = if (removable) MaterialTheme.spacing.xs else MaterialTheme.spacing.md,
                top = MaterialTheme.spacing.xs,
                bottom = MaterialTheme.spacing.xs,
            ),
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.metadata.copy(fontWeight = FontWeight.SemiBold),
            )
            if (removable) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = removeLabel,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HTagChipPreview() {
    HelloTheme {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                HTagChip(tag = "spanish")
                HTagChip(tag = "work", variant = BadgeVariant.Outline)
                HTagChip(tag = "HSK3", variant = BadgeVariant.Default, removable = true, onRemove = {})
                HTagChip(tag = "urgent", variant = BadgeVariant.Destructive, removable = true, onRemove = {})
            }
        }
    }
}
