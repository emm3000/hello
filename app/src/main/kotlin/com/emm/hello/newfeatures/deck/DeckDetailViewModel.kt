package com.emm.hello.newfeatures.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.ObserveFlashcardsWithReviewUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class DeckDetailViewModel(
    private val deckId: String,
    getDeckDetailUseCase: GetDeckDetailUseCase,
    private val observeFlashcardsWithReviewUseCase: ObserveFlashcardsWithReviewUseCase,
) : ViewModel() {

    // Renamed from `decks` → `uiState` (it holds a single deck's state, not a list of decks)
    val uiState: StateFlow<DeckDetailUiState> = combine(
        flow = getDeckDetailUseCase.fetch(deckId),
        flow2 = fetchSessionCards(),
        transform = { deck, (sessionCards, hasSessionEnabled) ->
            val mergedCards = mergeDeckCardsById(deck.cards, sessionCards)
            DeckDetailUiState(
                deck = deck.copy(cards = mergedCards),
                hasSessionEnabled = hasSessionEnabled,
            )
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeckDetailUiState(),
    )

    // Fixed typo: fetchSessionCars → fetchSessionCards
    private fun fetchSessionCards(): Flow<Pair<List<Flashcard>, Boolean>> =
        observeFlashcardsWithReviewUseCase.fetch(deckId).map { flashcards ->
            val hasSessionEnabled = flashcards.any { it.review.nextReviewAt <= Instant.now().toEpochMilli() }
            flashcards to hasSessionEnabled
        }
}

internal fun mergeDeckCardsById(
    deckCards: List<Flashcard>,
    sessionCards: List<Flashcard>,
): List<Flashcard> {
    val reviewsByCardId = sessionCards.associateBy(Flashcard::id)
    return deckCards.map { deckCard ->
        val sessionCard = reviewsByCardId[deckCard.id]
        if (sessionCard == null) deckCard else deckCard.copy(review = sessionCard.review)
    }
}
