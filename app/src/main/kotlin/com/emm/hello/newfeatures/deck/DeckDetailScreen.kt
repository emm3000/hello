package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.deck.Deck
import com.emm.domain.deck.Tag
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HSearchBar
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.core.ui.HTagChip
import com.emm.hello.core.ui.HSectionBlock
import com.emm.hello.core.ui.HStatCard
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DeckDetailTopBar(
                deck = state.deck,
                onNavigateBack = onNavigateBack,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
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
                start = MaterialTheme.spacing.lg,
                end = MaterialTheme.spacing.lg,
                top = MaterialTheme.spacing.sm,
                bottom = 88.dp,
            ),
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            item {
                DeckStatsHeader(
                    cardCount = state.deck.cards.size,
                    hasSessionEnabled = state.hasSessionEnabled,
                    tags = state.deck.tags.map { it.value },
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

@Composable
private fun DeckDetailTopBar(
    deck: Deck,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(
                    text = deck.name.ifBlank { stringResource(R.string.deck_detail_title_fallback) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (deck.description.isNotBlank()) {
                    Text(
                        text = deck.description,
                        style = MaterialTheme.typography.metadata,
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
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun DeckStatsHeader(
    cardCount: Int,
    hasSessionEnabled: Boolean,
    tags: List<String>,
    onReview: () -> Unit,
) {
    val deckPluralLabel = if (cardCount == 1) {
        stringResource(R.string.deck_plural_one)
    } else {
        stringResource(R.string.deck_plural_other)
    }
    val reviewStatusValue = if (hasSessionEnabled) {
        stringResource(R.string.review_status_ready)
    } else {
        stringResource(R.string.review_status_up_to_date)
    }
    val reviewSectionDescription = if (hasSessionEnabled) {
        stringResource(R.string.start_review)
    } else {
        stringResource(R.string.no_pending_cards)
    }
    val reviewBadgeVariant = if (hasSessionEnabled) BadgeVariant.Success else BadgeVariant.Secondary

    HSectionBlock(
        title = stringResource(R.string.review_status_label),
        description = reviewSectionDescription,
        trailingContent = {
            HBadge(
                label = reviewStatusValue,
                variant = reviewBadgeVariant,
            )
        },
        showDivider = false,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            if (tags.isNotEmpty()) {
                TagsRow(tags = tags)
            }

            HStatCard(
                value = "$cardCount",
                label = deckPluralLabel,
                modifier = Modifier.fillMaxWidth(),
            )

            if (hasSessionEnabled) {
                HButton(
                    text = stringResource(R.string.start_review),
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Filled.PlayArrow,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsRow(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        tags.sorted().forEach { tag ->
            HTagChip(tag = tag, variant = BadgeVariant.Tertiary)
        }
    }
}

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyCards(onAddCard: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        HSectionBlock(
            title = stringResource(R.string.empty_cards_title),
            description = stringResource(R.string.empty_cards_description),
            showDivider = false,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Outlined.HistoryEdu,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HButton(
                    text = stringResource(R.string.add_card),
                    onClick = onAddCard,
                    variant = ButtonVariant.Outline,
                )
            }
        }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = reviewDate.format(formatter),
                    style = MaterialTheme.typography.metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(card.id.value) },
    )
}

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
                                    ""
                                )
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
            )
        )
    }
}
