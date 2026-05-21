package com.emm.hello.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

enum class CardVariant { Elevated, Filled, Outlined }

/**
 * Card inspired by shadcn/ui with optional header/content/footer.
 *
 * Usage: DeckItem in Dashboard, CardPreview in NewCard, CardDetail in FlashcardDetail.
 */
@Composable
fun HCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Elevated,
    cornerRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (containerColor, tonalElevation, border) = cardTokens(variant)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = if (variant == CardVariant.Elevated) 2.dp else 0.dp,
        border = border,
    ) {
        Column(content = content)
    }
}

@Composable
private fun cardTokens(variant: CardVariant): Triple<Color, Dp, BorderStroke?> {
    val cs = MaterialTheme.colorScheme
    return when (variant) {
        CardVariant.Elevated -> Triple(cs.surfaceContainerLow, 0.dp, null)
        CardVariant.Filled -> Triple(cs.surfaceContainerHighest, 0.dp, null)
        CardVariant.Outlined -> Triple(cs.surface, 0.dp, BorderStroke(1.dp, cs.outlineVariant))
    }
}

/** Header with optional title and subtitle. */
@Composable
fun HCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Main content area of the card. */
@Composable
fun HCardContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 0.dp),
        content = content,
    )
}

/** Footer aligned to the right by default. */
@Composable
fun HCardFooter(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        content = content,
    )
}

@PreviewLightDark
@Composable
private fun HCardVariantsPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CardVariant.entries.forEach { variant ->
                    HCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = variant,
                    ) {
                        HCardContent {
                            Text(
                                text = variant.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HCardWithSlotsPreview() {
    HelloTheme {
        Surface {
            HCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                variant = CardVariant.Elevated,
            ) {
                HCardHeader(
                    title = "Serendipity",
                    description = "/ˌserənˈdɪpɪti/ · Noun",
                )
                HCardContent {
                    Text(
                        text = "The occurrence of events by chance in a happy or beneficial way.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                HCardFooter {
                    HButton(
                        text = "Ver detalle",
                        onClick = {},
                        variant = ButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
