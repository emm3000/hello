package com.emm.domain.curated

import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.ids.DeckId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetCuratedDecksUseCase(
    private val catalog: CuratedDeckCatalog,
    private val deckRepository: DeckRepository,
) {

    operator fun invoke(): Flow<List<CuratedDeckListing>> {
        return deckRepository.deckWithFlashcardCount().map { decks: List<Deck> ->
            toListings(decks.mapTo(mutableSetOf(), Deck::id))
        }
    }

    private fun toListings(liveDeckIds: Set<DeckId>): List<CuratedDeckListing> {
        return catalog.decks().map { curated: CuratedDeck ->
            CuratedDeckListing(
                deck = curated,
                installedDeckId = curated.installedDeckId.takeIf { it in liveDeckIds },
            )
        }
    }
}
