@file:OptIn(ExperimentalCoroutinesApi::class)

package com.emm.domain.deck

import com.emm.domain.flashcard.FlashcardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class DeckFetcher(
    private val repository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
) {

    fun fetch(): Flow<List<Deck>> {
        return repository.fetchAll().flatMapLatest(::combineDecksWithTheirCards)
    }

    private fun combineDecksWithTheirCards(decks: List<Deck>): Flow<List<Deck>> {
        if (decks.isEmpty()) {
            return flowOf(emptyList())
        }

        val individualDeckFlows: List<Flow<Deck>> = decks.map(::flowOfDeckWithCards)

        return combine(individualDeckFlows) { latestDecks: Array<Deck> ->
            latestDecks.toList()
        }
    }

    private fun flowOfDeckWithCards(deck: Deck): Flow<Deck> {
        return flashcardRepository.fetchByDeckId(deck.id)
            .map { flashcards ->
                deck.copy(cards = flashcards)
            }
    }

}