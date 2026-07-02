package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.emm.domain.flashcard.FsrsCard
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
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HChip
import com.emm.hello.core.ui.HDropdownMenu
import com.emm.hello.core.ui.HFab
import com.emm.hello.core.ui.HMenuItem
import com.emm.hello.core.ui.HSearchBar
import com.emm.hello.core.ui.HSectionLabel
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
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

    var searchExpanded by remember { mutableStateOf(false) }
    val searchVisible = searchExpanded || isSearching

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
                        bottom = 120.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item {
                        EmberDeckHeader(
                            deck = state.deck,
                            dueCount = dueCount,
                            onReview = onReview,
                        )
                        Spacer(Modifier.height(26.dp))
                    }

                    if (state.deck.cards.isEmpty()) {
                        item { EmptyCards(onAddCard = onAddCard) }
                    } else {
                        item {
                            CardsSectionHeader(
                                totalCount = state.deck.cards.size,
                                filteredCount = filteredCards.size,
                                isSearching = isSearching,
                                searchExpanded = searchVisible,
                                onToggleSearch = {
                                    if (searchVisible) {
                                        searchExpanded = false
                                        if (isSearching) onSearchChange("")
                                    } else {
                                        searchExpanded = true
                                    }
                                },
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        if (searchVisible) {
                            item {
                                HSearchBar(
                                    value = state.searchQuery,
                                    onValueChange = onSearchChange,
                                    placeholder = stringResource(R.string.search_cards_hint),
                                    clearContentDescription = stringResource(R.string.search_clear_content_description),
                                )
                                Spacer(Modifier.height(14.dp))
                            }
                        }

                        if (isSearching && filteredCards.isEmpty()) {
                            item { NoSearchResults(query = state.searchQuery.trim()) }
                        } else {
                            item {
                                GroupedCardList(
                                    cards = filteredCards,
                                    onCardClick = onCardClick,
                                )
                            }
                        }
                    }
                }
            }

            HFab(
                onClick = onAddCard,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp),
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp),
            )
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
                HDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    items = listOf(
                        HMenuItem(
                            label = stringResource(R.string.edit),
                            icon = Icons.Outlined.Edit,
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                        ),
                        HMenuItem(
                            label = stringResource(R.string.delete),
                            icon = Icons.Outlined.Delete,
                            isDestructive = true,
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                        ),
                    ),
                )
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
private fun StatSegment(text: String, color: Color) {
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
        if (card.review.reps > 0 && interval > 0) interval else null
    }
    if (intervals.isEmpty()) return null
    return intervals.average()
}

// ─────────────────────────────────────────────────────────────────────────────
// Cards section — HSectionLabel "Tarjetas · N" + inline search toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardsSectionHeader(
    totalCount: Int,
    filteredCount: Int,
    isSearching: Boolean,
    searchExpanded: Boolean,
    onToggleSearch: () -> Unit,
) {
    val baseLabel = stringResource(R.string.cards_section_label)
    val countSegment = if (isSearching) "$filteredCount / $totalCount" else "$totalCount"
    val label = "$baseLabel · $countSegment"
    val toggleDescription = if (searchExpanded) {
        stringResource(R.string.deck_detail_search_toggle_close)
    } else {
        stringResource(R.string.deck_detail_search_toggle_open)
    }

    HSectionLabel(
        label = label,
        action = {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = toggleDescription,
                    tint = if (searchExpanded) emberAccent else emberMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Grouped card list — single emberSurface column with hairline dividers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GroupedCardList(
    cards: List<Flashcard>,
    onCardClick: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = emberSurface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            cards.forEachIndexed { index, card ->
                CardListItem(
                    card = card,
                    onClick = { onCardClick(card.id.value) },
                )
                if (index != cards.lastIndex) {
                    HSeparator(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun CardListItem(
    card: Flashcard,
    onClick: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))
    val reviewDate = Instant.ofEpochMilli(card.review.nextReviewAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val supportingText = card.translation.ifBlank { card.phonetic }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.word,
                fontFamily = geist,
                fontWeight = FontWeight.Medium,
                fontSize = 15.5.sp,
                color = emberOnBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supportingText.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = supportingText,
                    fontFamily = instrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = emberMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.size(12.dp))

        Text(
            text = reviewDate.format(formatter),
            fontFamily = geistMono,
            fontSize = 11.sp,
            letterSpacing = 0.06.em,
            color = emberMuted,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty / no-results states (Ember)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoSearchResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.deck_detail_search_no_results, query),
            fontFamily = instrumentSerif,
            fontSize = 28.sp,
            color = emberOnBg,
        )
        Text(
            text = stringResource(R.string.search_no_results, query),
            fontFamily = geist,
            fontSize = 14.sp,
            color = emberMuted,
        )
    }
}

@Composable
private fun EmptyCards(onAddCard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.mascot_invite),
            contentDescription = null,
            modifier = Modifier.size(104.dp),
        )
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
            icon = Icons.Default.Add,
        )
    }
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
                            review = FsrsCard.new("1".toFlashcardId(), SystemClock),
                        ),
                        Flashcard.create(
                            id = "2".toFlashcardId(),
                            word = "Ephemeral",
                            meaning = "Lasting for a very short time",
                            translation = "Efímero",
                            examples = listOf(),
                            phonetic = "/ɪˈfem(ə)rəl/",
                            review = FsrsCard.new("1".toFlashcardId(), SystemClock),
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
