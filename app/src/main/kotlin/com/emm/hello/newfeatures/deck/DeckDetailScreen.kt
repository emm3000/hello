package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HSeparator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DeckDetailScreen(
    modifier: Modifier = Modifier,
    state: DeckDetailUiState = DeckDetailUiState(),
    onNavigateBack: () -> Unit = {},
    onReview: () -> Unit = {},
    onCardClick: (String) -> Unit = {},
    onAddCard: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.deck.name.ifBlank { stringResource(R.string.deck_detail_title_fallback) },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.deck.description.isNotBlank()) {
                            Text(
                                text = state.deck.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCard,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_card))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 88.dp,
            ),
            modifier = Modifier.padding(innerPadding),
        ) {
            // ── Header with statistics ──────────────────────────────────────
            item {
                DeckStatsHeader(
                    cardCount = state.deck.cards.size,
                    hasSessionEnabled = state.hasSessionEnabled,
                    onReview = onReview,
                )
            }

            // ── Empty list ──────────────────────────────────────────────────
            if (state.deck.cards.isEmpty()) {
                item {
                    EmptyCards(onAddCard = onAddCard)
                }
            }

            // ── Cards ─────────────────────────────────────────────────────
            if (state.deck.cards.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.cards_section_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HBadge(
                            label = "${state.deck.cards.size}",
                            variant = BadgeVariant.Secondary,
                        )
                    }
                }

                items(state.deck.cards, key = Flashcard::id) { card ->
                    DeckCardItem(card = card, onCardClick = { onCardClick(it) })
                    HSeparator()
                }
            }
        }
    }
}

// ── Component: Deck statistics ────────────────────────────────────────

@Composable
private fun DeckStatsHeader(
    cardCount: Int,
    hasSessionEnabled: Boolean,
    onReview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                value = "$cardCount",
                label = if (cardCount == 1) stringResource(R.string.deck_plural_one) else stringResource(R.string.deck_plural_other),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = if (hasSessionEnabled) stringResource(R.string.review_status_ready) else stringResource(R.string.review_status_up_to_date),
                label = stringResource(R.string.review_status_label),
                highlight = hasSessionEnabled,
                modifier = Modifier.weight(1f),
            )
        }

        // Botón de repaso
        HButton(
            text = if (hasSessionEnabled) stringResource(R.string.start_review) else stringResource(R.string.no_pending_cards),
            onClick = onReview,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = hasSessionEnabled,
            leadingIcon = Icons.Filled.PlayArrow,
            variant = if (hasSessionEnabled) ButtonVariant.Default else ButtonVariant.Secondary,
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (highlight) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

// ── Component: Empty cards state ─────────────────────────────────────

@Composable
private fun EmptyCards(onAddCard: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.HistoryEdu,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.empty_cards_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.empty_cards_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            HButton(
                text = stringResource(R.string.add_card),
                onClick = onAddCard,
                variant = ButtonVariant.Outline,
            )
        }
    }
}

// ── Component: Card row ───────────────────────────────────────────────

@Composable
private fun DeckCardItem(
    card: Flashcard,
    onCardClick: (String) -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val reviewDate = Instant.ofEpochSecond(card.review.nextReviewAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    ListItem(
        headlineContent = {
            Text(
                text = card.word,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            val hint = card.phonetic.ifBlank { card.translation }
            if (hint.isNotEmpty()) {
                Text(
                    text = hint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = reviewDate.format(formatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(card.id) },
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun DeckDetailScreenPreview() {
    HelloTheme {
        DeckDetailScreen(
            state = DeckDetailUiState(
                deck = Deck.Empty.copy(
                    name = "Vocabulario Avanzado",
                    description = "Palabras de nivel C1",
                    cards = listOf(
                        Flashcard(
                            id = "1",
                            word = "Serendipity",
                            meaning = "The occurrence of events by chance in a happy way",
                            translation = "Casualidad afortunada",
                            examples = listOf(
                                Example(
                                    "1",
                                    "Finding that book was pure serendipity",
                                    "Encontrar ese libro fue pura casualidad",
                                    ""
                                )
                            ),
                            phonetic = "/ˌserənˈdɪpɪti/",
                            review = FlashcardReview.Empty,
                        ),
                        Flashcard(
                            id = "2",
                            word = "Ephemeral",
                            meaning = "Lasting for a very short time",
                            translation = "Efímero",
                            examples = listOf(),
                            phonetic = "/ɪˈfem(ə)rəl/",
                            review = FlashcardReview.Empty,
                        ),
                    ),
                ),
                hasSessionEnabled = true,
            )
        )
    }
}
