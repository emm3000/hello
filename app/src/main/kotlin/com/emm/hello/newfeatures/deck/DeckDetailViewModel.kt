package com.emm.hello.newfeatures.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DecksWithCardsProvider
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardAndReviewFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

data class DeckDetailUiState(
    val deck: Deck = Deck.Empty,
    val cardsSession: List<Flashcard> = emptyList(),
    val hasSessionEnabled: Boolean = false,
)

class DeckDetailViewModel(
    private val deckId: String,
    decksWithCardsProvider: DecksWithCardsProvider,
    private val flashcardAndReviewFetcher: FlashcardAndReviewFetcher,
) : ViewModel() {

    val decks: StateFlow<DeckDetailUiState> = combine(
        flow = decksWithCardsProvider.provide(deckId),
        flow2 = fetchSessionCars(),
        transform = { deck, (cardsSession, hasSessionEnabled) ->
            DeckDetailUiState(
                deck = deck,
                cardsSession = cardsSession,
                hasSessionEnabled = hasSessionEnabled,
            )
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeckDetailUiState(),
    )

    private fun fetchSessionCars(): Flow<Pair<List<Flashcard>, Boolean>> = flow {
        val fetchAll: List<Flashcard> = flashcardAndReviewFetcher.fetch(deckId)
            .map { flashcard -> flashcard.copy(id = "${flashcard.id}${flashcard.review.nextReviewAt}") }
        val hasSessionEnabled = fetchAll.any { it.review.nextReviewAt <= Instant.now().epochSecond }
        emit(Pair(fetchAll, hasSessionEnabled))
    }.onStart { emit(Pair(emptyList(), false)) }
}