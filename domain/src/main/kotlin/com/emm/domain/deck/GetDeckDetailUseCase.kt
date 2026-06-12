package com.emm.domain.deck

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.ids.toDeckId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetDeckDetailUseCase(
    private val repository: DeckRepository,
    private val cardRepository: FlashcardRepository,
) {

    operator fun invoke(deckId: String): Flow<Deck?> {
        val typedDeckId = deckId.toDeckId()

        return combine(
            flow = repository.fetchById(typedDeckId),
            flow2 = cardRepository.fetchByDeckId(typedDeckId)
        ) { deck: Deck?, cards: List<Flashcard> ->
            deck?.copy(cards = cards)
        }
    }
}
