package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.deck.Deck
import com.emm.domain.deck.Tag
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.emberSurface
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HChip
import com.emm.hello.core.ui.HSearchBar
import com.emm.hello.core.ui.HSectionBlock
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.core.ui.HTopBar
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
    onSearchChange: (String) -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissDelete: () -> Unit = {},
) {
    val isSearching = state.searchQuery.trim().isNotEmpty()
    val filteredCards = state.deck.cards.filter { card ->
        matchesSearchQuery(card, state.searchQuery)
    }
    val now = Instant.now().toEpochMilli()
    val dueCount = state.deck.cards.count { it.review.nextReviewAt <= now }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = emberBg,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                DeckDetailTopBar(
                    onNavigateBack = onNavigateBack,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                )

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    item {
                        EmberDeckHeader(
                            deck = state.deck,
                            dueCount = dueCount,
                            onReview = onReview,
                        )
                    }

                    stickyHeader {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = onSearchChange,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (state.deck.cards.isEmpty()) {
                        item {
                            EmptyCards(onAddCard = onAddCard)
                        }
                    }

                    if (state.deck.cards.isNotEmpty()) {
                        item {
                            HSectionBlock(
                                title = stringResource(R.string.cards_section_label),
                                modifier = Modifier.padding(vertical = MaterialTheme.spacing.sm),
                                trailingContent = {
                                    val label = if (isSearching) {
                                        "${filteredCards.size} / ${state.deck.cards.size}"
                                    } else {
                                        "${state.deck.cards.size}"
                                    }
                                    HBadge(
                                        label = label,
                                        variant = BadgeVariant.Secondary,
                                    )
                                },
                                showDivider = false,
                            )
                        }

                        if (filteredCards.isEmpty() && isSearching) {
                            item {
                                NoSearchResults(query = state.searchQuery.trim())
                            }
                        } else {
                            items(filteredCards, key = { card -> card.id.value }) { card ->
                                DeckCardItem(card = card, onCardClick = { onCardClick(it) })
                                HSeparator()
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onAddCard,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_card))
            }
        }
    }

    if (state.isDeleteConfirmationVisible) {
        HAlertDialog(
            title = stringResource(R.string.delete_deck_title),
            description = stringResource(R.string.delete_deck_description),
            icon = Icons.Outlined.Delete,
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            isDangerous = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar — Ember (back + more dropdown)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeckDetailTopBar(
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    HTopBar(
        onBack = onNavigateBack,
        actions = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = emberPrimary,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEditClick()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    )
                }
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Ember deck header — mono label · 44sp serif name · description · tags · stats · CTA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmberDeckHeader(
    deck: Deck,
    dueCount: Int,
    onReview: () -> Unit,
) {
    val cardCount = deck.cards.size
    val avgIntervalDays = averageIntervalDays(deck.cards)
    val deckName = deck.name.ifBlank { stringResource(R.string.deck_detail_title_fallback) }
    val tagLabels = deck.tags.map { it.value }.sorted()

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.deck_detail_meta_label).uppercase(),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = deckName,
            fontFamily = instrumentSerif,
            fontSize = 44.sp,
            lineHeight = (44 * 1.04f).sp,
            letterSpacing = (-0.5).sp,
            color = emberPrimary,
        )

        if (deck.description.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = deck.description,
                fontFamily = geist,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = emberMuted,
            )
        }

        if (tagLabels.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            DeckTagsRow(tags = tagLabels)
        }

        Spacer(Modifier.height(18.dp))
        StatsRow(
            cardCount = cardCount,
            dueCount = dueCount,
            avgIntervalDays = avgIntervalDays,
        )

        if (dueCount > 0) {
            Spacer(Modifier.height(14.dp))
            HButton(
                text = stringResource(R.string.deck_detail_study_now_cta, dueCount),
                onClick = onReview,
                variant = HButtonVariant.Accent,
                size = HButtonSize.Lg,
                full = true,
            )
        }

        Spacer(Modifier.height(10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckTagsRow(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { tag ->
            HChip(label = tag)
        }
    }
}

@Composable
private fun StatsRow(
    cardCount: Int,
    dueCount: Int,
    avgIntervalDays: Double?,
) {
    val cardsLabel = pluralStringResource(R.plurals.deck_detail_stats_cards, cardCount, cardCount)
    val dueLabel = stringResource(R.string.deck_detail_stats_due_today, dueCount)
    val avgLabel = avgIntervalDays?.let { value ->
        val formatted = "%.1f".format(Locale.US, value)
        stringResource(R.string.deck_detail_stats_avg_interval, formatted)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = emberDivider,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatSegment(text = cardsLabel, color = emberOnBg)
        StatSeparator()
        StatSegment(text = dueLabel, color = emberAccent)
        if (avgLabel != null) {
            StatSeparator()
            StatSegment(text = avgLabel, color = emberMuted)
        }
    }
}

@Composable
private fun StatSegment(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        fontFamily = geistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.06.em,
        color = color,
    )
}

@Composable
private fun StatSeparator() {
    Text(
        text = " · ",
        fontFamily = geistMono,
        fontSize = 12.sp,
        color = emberMuted,
    )
}

private fun averageIntervalDays(cards: List<Flashcard>): Double? {
    val intervals = cards.mapNotNull { card ->
        val interval = card.review.interval
        if (card.review.repetitions > 0 && interval > 0) interval else null
    }
    if (intervals.isEmpty()) return null
    return intervals.average()
}

// ─────────────────────────────────────────────────────────────────────────────
// Card list helpers (unchanged in sub-1 — sub-2 will replace)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    HSearchBar(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = stringResource(R.string.search_cards_hint),
        clearContentDescription = stringResource(R.string.search_clear_content_description),
    )
}

@Composable
private fun NoSearchResults(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.search_no_results, query),
            style = MaterialTheme.typography.bodyLarge,
            color = emberMuted,
        )
    }
}

@Composable
private fun EmptyCards(onAddCard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Text(
            text = stringResource(R.string.empty_cards_title),
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 28.sp,
            color = emberOnBg,
        )
        Text(
            text = stringResource(R.string.empty_cards_description),
            fontFamily = geist,
            fontSize = 14.sp,
            color = emberMuted,
        )
        HButton(
            text = stringResource(R.string.add_card),
            onClick = onAddCard,
            variant = HButtonVariant.Accent,
            size = HButtonSize.Md,
        )
    }
}

@Composable
private fun DeckCardItem(
    card: Flashcard,
    onCardClick: (String) -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val reviewDate = Instant.ofEpochMilli(card.review.nextReviewAt)
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
                    color = emberMuted,
                    style = MaterialTheme.typography.metadata,
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = emberMuted,
                )
                Text(
                    text = reviewDate.format(formatter),
                    style = MaterialTheme.typography.metadata,
                    color = emberMuted,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = emberSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(card.id.value) },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun DeckDetailScreenPreview() {
    HelloTheme {
        DeckDetailScreen(
            state = DeckDetailUiState(
                deck = Deck.empty(SystemClock).copy(
                    name = "Vocabulario Avanzado",
                    description = "Palabras de nivel C1",
                    tags = listOf(Tag("spanish"), Tag("advanced")),
                    cards = listOf(
                        Flashcard.create(
                            id = "1".toFlashcardId(),
                            word = "Serendipity",
                            meaning = "The occurrence of events by chance in a happy way",
                            translation = "Casualidad afortunada",
                            examples = listOf(
                                Example(
                                    "1",
                                    "Finding that book was pure serendipity",
                                    "Encontrar ese libro fue pura casualidad",
                                    "",
                                ),
                            ),
                            phonetic = "/ˌserənˈdɪpɪti/",
                            review = FlashcardReview.empty(SystemClock),
                        ),
                        Flashcard.create(
                            id = "2".toFlashcardId(),
                            word = "Ephemeral",
                            meaning = "Lasting for a very short time",
                            translation = "Efímero",
                            examples = listOf(),
                            phonetic = "/ɪˈfem(ə)rəl/",
                            review = FlashcardReview.empty(SystemClock),
                        ),
                    ),
                ),
                hasSessionEnabled = true,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun DeckDetailEmptyPreview() {
    HelloTheme {
        DeckDetailScreen(
            state = DeckDetailUiState(
                deck = Deck.empty(SystemClock).copy(
                    name = "Inglés básico",
                    description = "",
                    tags = emptyList(),
                ),
            ),
        )
    }
}
