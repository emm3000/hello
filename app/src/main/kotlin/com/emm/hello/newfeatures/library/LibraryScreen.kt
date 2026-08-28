package com.emm.hello.newfeatures.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.library.LibraryFlashcard
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.schibsted
import com.emm.hello.core.theme.destructiveInk
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.warningInk
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HChip
import com.emm.hello.core.ui.HEmptyState
import com.emm.hello.core.ui.HLoadingSpinner
import com.emm.hello.core.ui.HSearchBar
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.core.ui.HTopBar
import java.time.LocalDateTime

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    state: LibraryUiState = LibraryUiState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onIntent: (LibraryUiIntent) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            HTopBar(
                onBack = onBack,
                title = stringResource(R.string.library_title),
                actions = { CardCounter(count = state.cards.size) },
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(6.dp))
                HSearchBar(
                    value = state.query,
                    onValueChange = { value -> onIntent(LibraryUiIntent.QueryChanged(value)) },
                    placeholder = stringResource(R.string.library_search_placeholder),
                    clearContentDescription = stringResource(R.string.library_search_clear_content_description),
                    leadingIconContentDescription = stringResource(R.string.library_search_icon_content_description),
                )

                if (state.decks.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    DeckFilterRow(
                        decks = state.decks,
                        selectedDeckId = state.selectedDeckId,
                        onDeckToggled = { deckId -> onIntent(LibraryUiIntent.DeckFilterToggled(deckId)) },
                        onClear = { onIntent(LibraryUiIntent.FiltersCleared) },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            when {
                state.isLoading -> LoadingContent(modifier = Modifier.weight(1f))

                state.isLibraryEmpty -> HEmptyState(
                    modifier = Modifier.weight(1f),
                    headline = stringResource(R.string.library_empty_headline),
                    accentWord = stringResource(R.string.library_empty_accent_word),
                    body = stringResource(R.string.library_empty_body),
                    primaryCta = {
                        HButton(
                            text = stringResource(R.string.library_empty_cta),
                            onClick = { onIntent(LibraryUiIntent.CaptureRequested) },
                            variant = HButtonVariant.Accent,
                        )
                    },
                )

                state.hasNoResults -> HEmptyState(
                    modifier = Modifier.weight(1f),
                    headline = stringResource(R.string.library_no_results_headline),
                    body = stringResource(R.string.library_no_results_body),
                    ghostCta = {
                        HButton(
                            text = stringResource(R.string.library_no_results_cta),
                            onClick = { onIntent(LibraryUiIntent.FiltersCleared) },
                            variant = HButtonVariant.Ghost,
                        )
                    },
                )

                else -> CardList(
                    modifier = Modifier.weight(1f),
                    cards = state.cards,
                    showDeckName = state.selectedDeckId == null,
                    onCardClick = { card -> onIntent(LibraryUiIntent.CardOpened(card)) },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun CardCounter(count: Int) {
    Text(
        text = pluralStringResource(R.plurals.library_card_counter, count, count),
        fontFamily = schibsted,
        fontWeight = FontWeight.Normal,
        fontSize = 10.5.sp,
        letterSpacing = 0.14.em,
        color = inkFaint,
        modifier = Modifier.padding(end = 18.dp),
    )
}

@Composable
private fun DeckFilterRow(
    decks: List<Deck>,
    selectedDeckId: DeckId?,
    onDeckToggled: (DeckId) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val allActive: Boolean = selectedDeckId == null
        HChip(
            label = stringResource(R.string.library_chip_all),
            active = allActive,
            onClick = { if (!allActive) onClear() },
        )
        decks.forEach { deck ->
            HChip(
                label = deck.name,
                active = selectedDeckId == deck.id,
                onClick = { onDeckToggled(deck.id) },
            )
        }
    }
}

@Composable
private fun CardList(
    cards: List<LibraryFlashcard>,
    showDeckName: Boolean,
    onCardClick: (LibraryFlashcard) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(cards, key = { it.id.value }) { card ->
            LibraryRow(
                card = card,
                showDeckName = showDeckName,
                onClick = { onCardClick(card) },
            )
            HSeparator(modifier = Modifier.padding(horizontal = 20.dp))
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun LibraryRow(
    card: LibraryFlashcard,
    showDeckName: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.word,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = ink,
                letterSpacing = (-0.1).sp,
            )
            if (card.translation.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = card.translation,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = inkMuted,
                    lineHeight = (13 * 1.35f).sp,
                )
            }
        }

        RowMarker(card = card, showDeckName = showDeckName)
    }
}

@Composable
private fun RowMarker(card: LibraryFlashcard, showDeckName: Boolean) {
    val statusLabel: String? = when (card.enrichmentStatus) {
        EnrichmentStatus.PENDING -> stringResource(R.string.library_status_pending)
        EnrichmentStatus.FAILED -> stringResource(R.string.library_status_failed)
        EnrichmentStatus.ENRICHED -> null
    }
    val statusColor: Color = when (card.enrichmentStatus) {
        EnrichmentStatus.PENDING -> warningInk
        EnrichmentStatus.FAILED -> destructiveInk
        EnrichmentStatus.ENRICHED -> inkFaint
    }
    val label: String? = statusLabel ?: card.deckName.takeIf { showDeckName && it.isNotBlank() }
    if (label == null) return

    Text(
        text = label,
        fontFamily = schibsted,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.1.em,
        color = statusColor,
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        HLoadingSpinner(size = 28.dp, color = inkMuted, strokeWidth = 2.dp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090A)
@Composable
private fun LibraryScreenPreview() {
    HelloTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreen(
                state = LibraryUiState(
                    isLoading = false,
                    decks = listOf(previewDeck("deck-1", "Viajes"), previewDeck("deck-2", "Trabajo")),
                    cards = listOf(
                        previewCard("1", "compelling", "convincente", "Viajes", EnrichmentStatus.ENRICHED),
                        previewCard("2", "leverage", "aprovechar", "Trabajo", EnrichmentStatus.ENRICHED),
                        previewCard("3", "brittle", "", "Trabajo", EnrichmentStatus.PENDING),
                        previewCard("4", "hoard", "", "Viajes", EnrichmentStatus.FAILED),
                    ),
                ),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090A)
@Composable
private fun LibraryScreenEmptyPreview() {
    HelloTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreen(state = LibraryUiState(isLoading = false))
        }
    }
}

private val previewCreatedAt: LocalDateTime = LocalDateTime.parse("2026-01-01T00:00:00")

private fun previewDeck(id: String, name: String): Deck = Deck(
    id = id.toDeckId(),
    name = name,
    description = "",
    createdAt = previewCreatedAt,
    cards = emptyList(),
    cardsCount = 0L,
)

private fun previewCard(
    id: String,
    word: String,
    translation: String,
    deckName: String,
    status: EnrichmentStatus,
): LibraryFlashcard = LibraryFlashcard(
    id = id.toFlashcardId(),
    deckId = "deck-1".toDeckId(),
    deckName = deckName,
    word = word,
    translation = translation,
    meaning = "",
    enrichmentStatus = status,
    nextReviewAt = null,
)
