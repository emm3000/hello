package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.semanticColors
import com.emm.hello.core.theme.spacing

enum class StatCardStatus { Default, Success, Warning, Destructive }

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    status: StatCardStatus = StatCardStatus.Default,
) {
    val (containerColor, valueColor, labelColor) = statCardColors(status)

    Surface(
        modifier = modifier,
        shape = MaterialTheme.helloShapes.container,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.lg,
                vertical = MaterialTheme.spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.metadata,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun statCardColors(status: StatCardStatus): Triple<Color, Color, Color> {
    val colorScheme = MaterialTheme.colorScheme
    return when (status) {
        StatCardStatus.Default -> Triple(
            colorScheme.surfaceContainerHigh,
            colorScheme.onSurface,
            colorScheme.onSurfaceVariant,
        )
        StatCardStatus.Success -> Triple(
            MaterialTheme.semanticColors.success.container,
            MaterialTheme.semanticColors.success.content,
            MaterialTheme.semanticColors.success.accent,
        )
        StatCardStatus.Warning -> Triple(
            MaterialTheme.semanticColors.warning.container,
            MaterialTheme.semanticColors.warning.content,
            MaterialTheme.semanticColors.warning.accent,
        )
        StatCardStatus.Destructive -> Triple(
            MaterialTheme.semanticColors.destructive.container,
            MaterialTheme.semanticColors.destructive.content,
            MaterialTheme.semanticColors.destructive.accent,
        )
    }
}

@PreviewLightDark
@Composable
private fun StatCardPreview() {
    HelloTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(value = "24", label = "tarjetas")
            StatCard(value = "Listo", label = "estado de repaso", status = StatCardStatus.Success)
            StatCard(value = "Advertencia", label = "estado", status = StatCardStatus.Warning)
        }
    }
}
