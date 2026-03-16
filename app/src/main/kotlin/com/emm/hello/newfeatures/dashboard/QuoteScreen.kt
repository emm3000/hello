package com.emm.hello.newfeatures.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.quote.Quote
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HSeparator

private const val COLLAPSE_ROTATION_DEGREES = 180f

@Composable
fun QuotesScreen(
    quotes: List<Quote>,
    createCard: (Quote) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        items(quotes, key = Quote::id) { quote ->
            QuoteItem(quote = quote, onClick = createCard)
        }
    }
}

@Composable
private fun QuoteItem(quote: Quote, onClick: (Quote) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    HCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        variant = CardVariant.Outlined,
    ) {
        val expandContentDescription = if (expanded) {
            stringResource(R.string.collapse_quote)
        } else {
            stringResource(R.string.expand_quote)
        }
        Column(modifier = Modifier.padding(16.dp)) {
            // -- Header (Always visible) -----------------------------------------------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = quote.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (quote.hasCard) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.already_has_flashcard),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = quote.phrase,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = expandContentDescription,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .rotate(if (expanded) COLLAPSE_ROTATION_DEGREES else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // -- Expandable Content ----------------------------------------------------
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HSeparator()
                    Spacer(Modifier.height(12.dp))

                    // Pronunciation + formality badge
                    if (quote.pronunciation.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = quote.pronunciation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontStyle = FontStyle.Italic,
                            )
                            if (quote.formality.isNotBlank()) {
                                HBadge(
                                    label = quote.formality,
                                    variant = BadgeVariant.Secondary,
                                )
                            }
                        }
                    }

                    // Translation
                    if (quote.translation.isNotBlank()) {
                        QuoteDetailRow(
                            icon = Icons.Default.Translate,
                            label = stringResource(R.string.translation_label),
                            content = quote.translation,
                        )
                    }

                    // Usage example
                    if (quote.example.isNotBlank()) {
                        QuoteDetailRow(
                            icon = Icons.Default.School,
                            label = stringResource(R.string.examples_label),
                            content = "\"${quote.example}\"",
                        )
                    }

                    // Tags
                    if (quote.tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            quote.tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HSeparator()
                    Spacer(Modifier.height(12.dp))

                    // CTA: Create flashcard
                    if (quote.hasCard) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                text = stringResource(R.string.quote_translated_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    } else {
                        HButton(
                            text = stringResource(R.string.generate_card),
                            onClick = { onClick(quote) },
                            variant = ButtonVariant.Default,
                            leadingIcon = Icons.Default.BookmarkAdd,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(showBackground = true, name = "Quotes Screen")
@Composable
private fun QuotesScreenPreview() {
    HelloTheme {
        QuotesScreen(quotes = sampleQuotes)
    }
}

@Preview(showBackground = true, name = "Quotes Screen Dark")
@Composable
private fun QuotesScreenPreviewDark() {
    HelloTheme(darkTheme = true) {
        QuotesScreen(quotes = sampleQuotes)
    }
}

private val sampleQuotes = listOf(
    Quote(
        id = "1",
        title = "Carpe Diem",
        phrase = "Seize the day",
        description = "A Latin aphorism, usually translated 'seize the day'.",
        translation = "Aprovecha el día",
        example = "I'm going to go skydiving — carpe diem!",
        context = "Motivational",
        pronunciation = "/ˌkɑːrpeɪ ˈdiːɛm/",
        formality = "Informal",
        tags = listOf("motivation", "latin", "classic"),
        category = "Philosophy",
        hasCard = false,
    ),
    Quote(
        id = "2",
        title = "Veni, Vidi, Vici",
        phrase = "I came, I saw, I conquered",
        description = "A Latin phrase attributed to Julius Caesar.",
        translation = "Vine, vi, vencí",
        example = "After the successful product launch, the CEO said 'Veni, vidi, vici'.",
        context = "Achievement",
        pronunciation = "/ˈweːniː ˈwiːdiː ˈwiːkiː/",
        formality = "Formal",
        tags = listOf("history", "latin", "victory"),
        category = "History",
        hasCard = true,
    ),
)
