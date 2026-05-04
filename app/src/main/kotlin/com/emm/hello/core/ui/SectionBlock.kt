package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

@Composable
fun SectionBlock(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.metadata,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            trailingContent?.invoke()
        }

        content()

        if (showDivider) {
            HSeparator()
        }
    }
}

@PreviewLightDark
@Composable
private fun SectionBlockPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionBlock(
                    title = "Entrada",
                    description = "Describe lo justo para que la IA entienda el objetivo.",
                ) {
                    HInput(
                        value = "serendipity",
                        onValueChange = {},
                        label = "Palabra o frase",
                    )
                }

                SectionBlock(
                    title = "Tarjetas",
                    trailingContent = {
                        HBadge(label = "12", variant = BadgeVariant.Secondary)
                    },
                    showDivider = false,
                )
            }
        }
    }
}
