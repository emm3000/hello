package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.emm.hello.core.theme.HelloTheme

@Composable
fun HBadgeGroup(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() },
    )
}

@PreviewLightDark
@Composable
private fun HBadgeGroupPreview() {
    HelloTheme {
        Surface {
            HBadgeGroup {
                HBadge(label = "Recognition", variant = BadgeVariant.Secondary)
                HBadge(label = "Production", variant = BadgeVariant.Outline)
                HBadge(label = "Cloze", variant = BadgeVariant.Success)
                HBadge(label = "Form", variant = BadgeVariant.Secondary)
            }
        }
    }
}
