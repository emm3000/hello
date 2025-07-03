package com.emm.hello.newfeatures.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.DecksWithCardsProvider
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardAndReviewFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class DeckDetailViewModel(
    private val deckId: String,
    decksWithCardsProvider: DecksWithCardsProvider,
    private val flashcardAndReviewFetcher: FlashcardAndReviewFetcher,
) : ViewModel() {

    val decks: StateFlow<DeckDetailUiState> = combine(
        flow = decksWithCardsProvider.provide(deckId),
        flow2 = fetchSessionCars(),
        transform = { deck, (cardsSession, hasSessionEnabled) ->
            val deckCardsWithReview: List<Flashcard> = deck.cards.zip(cardsSession) { deckCards: Flashcard, sessionCards: Flashcard ->
                Flashcard(
                    id = deckCards.id,
                    word = deckCards.word,
                    meaning = deckCards.meaning,
                    translation = deckCards.translation,
                    examples = deckCards.examples,
                    phonetic = deckCards.phonetic,
                    review = sessionCards.review,
                )
            }
            DeckDetailUiState(
                deck = deck.copy(cards = deckCardsWithReview),
                hasSessionEnabled = hasSessionEnabled,
            )
        }
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeckDetailUiState(),
    )

    private fun fetchSessionCars(): Flow<Pair<List<Flashcard>, Boolean>> {
        return flashcardAndReviewFetcher.fetch(deckId)
            .map {
                val updatedFlashcards: List<Flashcard> = it.map(::copyWithUpdatedId)
                val reviewAvailable: Boolean = updatedFlashcards.any(::isReviewDue)
                Pair(updatedFlashcards, reviewAvailable)
            }
    }

    private fun copyWithUpdatedId(
        flashcard: Flashcard,
    ): Flashcard = flashcard.copy(id = "${flashcard.id}${flashcard.review.nextReviewAt}")

    private fun isReviewDue(
        flashcard: Flashcard,
    ): Boolean = flashcard.review.nextReviewAt <= Instant.now().epochSecond
}