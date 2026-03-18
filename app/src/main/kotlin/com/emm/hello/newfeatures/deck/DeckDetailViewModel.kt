package com.emm.hello.newfeatures.deck

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.ObserveFlashcardsWithReviewUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Instant

class DeckDetailViewModel(
    private val deckId: String,
    getDeckDetailUseCase: GetDeckDetailUseCase,
    private val observeFlashcardsWithReviewUseCase: ObserveFlashcardsWithReviewUseCase,
) : MviViewModel<DeckDetailUiState, DeckDetailUiIntent, DeckDetailUiEffect>(
    initialState = DeckDetailUiState(),
) {

    init {
        // Renamed from `decks` → `uiState` (it holds a single deck's state, not a list of decks)
        combine(
            flow = getDeckDetailUseCase(deckId),
            flow2 = fetchSessionCards(),
            transform = { deck, (sessionCards, hasSessionEnabled) ->
                val mergedCards = mergeDeckCardsById(deck.cards, sessionCards)
                mutableState.value.copy(
                    deck = deck.copy(cards = mergedCards),
                    hasSessionEnabled = hasSessionEnabled,
                )
            }
        ).onEach { mutableState.value = it }.launchIn(viewModelScope)
    }

    // Fixed typo: fetchSessionCars → fetchSessionCards
    private fun fetchSessionCards(): Flow<Pair<List<Flashcard>, Boolean>> =
        observeFlashcardsWithReviewUseCase(deckId).map { flashcards ->
            val hasSessionEnabled = flashcards.any { it.review.nextReviewAt <= Instant.now().toEpochMilli() }
            flashcards to hasSessionEnabled
        }

    override fun onIntent(intent: DeckDetailUiIntent) = Unit
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
