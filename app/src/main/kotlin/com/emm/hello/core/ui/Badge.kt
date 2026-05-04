package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.semanticColors
import com.emm.hello.core.theme.spacing

// ─── Variants ──────────────────────────────────────────────────────────────

enum class BadgeVariant { Default, Secondary, Destructive, Outline, Warning, Success }

/**
 * Status badge/chip inspired by shadcn/ui.
 *
 * Typical usage: cards count in a Deck, review status,
 * flashcard category, difficulty.
 */
@Composable
fun HBadge(
    label: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Default,
) {
    val (containerColor, contentColor) = badgeColors(variant)
    val stateDescription = badgeStateDescription(variant)

    Surface(
        modifier = modifier.semantics {
            if (stateDescription != null) {
                this.stateDescription = stateDescription
            }
        },
        shape = MaterialTheme.helloShapes.pill,
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
            style = MaterialTheme.typography.metadata.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.md,
                vertical = MaterialTheme.spacing.xs,
            ),
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
        BadgeVariant.Warning ->
            MaterialTheme.semanticColors.warning.container to MaterialTheme.semanticColors.warning.content
        // Success → tertiary (green in new theme)
        BadgeVariant.Success -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
}

@Composable
private fun badgeStateDescription(variant: BadgeVariant): String? {
    return when (variant) {
        BadgeVariant.Default,
        BadgeVariant.Secondary,
        BadgeVariant.Outline -> null
        BadgeVariant.Destructive -> stringResource(R.string.error_label)
        BadgeVariant.Warning -> stringResource(R.string.warning_label)
        BadgeVariant.Success -> stringResource(R.string.success_label)
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
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
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
