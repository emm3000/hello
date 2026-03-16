package com.emm.domain.deck

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetDeckDetailUseCase(
    private val repository: DeckRepository,
    private val cardRepository: FlashcardReadRepository,
) {

    operator fun invoke(deckId: String): Flow<Deck> = combine(
        flow = repository.findById(deckId),
        flow2 = cardRepository.fetchByDeckId(deckId)
    ) { deck: Deck, cards: List<Flashcard> ->
        deck.copy(cards = cards)
    }.map { deck ->
        val newFlashcards: List<Flashcard> = deck.cards.map { flashcard ->
            cardRepository.fetchById(flashcard.id)
        }
        deck.copy(cards = newFlashcards)
    }
}
