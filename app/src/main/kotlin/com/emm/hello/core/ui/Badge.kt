package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
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

// ─── Variantes ──────────────────────────────────────────────────────────────

enum class BadgeVariant { Default, Secondary, Destructive, Outline, Success }

/**
 * Badge/chip de estado inspirado en shadcn/ui.
 *
 * Uso típico: conteo de cards en un Deck, estado de revisión,
 * categoría de una flashcard, dificultad.
 */
@Composable
fun HBadge(
    label: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Default,
) {
    val (containerColor, contentColor) = badgeColors(variant)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = containerColor,
        contentColor = contentColor,
        border = if (variant == BadgeVariant.Outline) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            null
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun badgeColors(variant: BadgeVariant): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (variant) {
        // Default → primary (black bg, white text)
        BadgeVariant.Default -> cs.primary to cs.onPrimary
        // Secondary → muted gray bg
        BadgeVariant.Secondary -> cs.surfaceContainerHighest to cs.onSurface
        // Destructive → error container (soft red)
        BadgeVariant.Destructive -> cs.errorContainer to cs.onErrorContainer
        // Outline → transparent + foreground text
        BadgeVariant.Outline -> Color.Transparent to cs.onSurface
        // Success → tertiary (green in new theme)
        BadgeVariant.Success -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun HBadgeVariantsPreview() {
    HelloTheme {
        Surface {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BadgeVariant.entries.forEach { variant ->
                    HBadge(
                        label = variant.name,
                        variant = variant,
                        modifier = Modifier.wrapContentWidth(),
                    )
                }
            }
        }
    }
}
