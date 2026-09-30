package com.emm.domain.deck

import com.emm.domain.curated.isInstalledCuratedDeck
import com.emm.domain.ids.DeckId

data class CaptureDeckChoice(
    val candidates: List<Deck>,
    val target: Deck?,
)

class ResolveCaptureDeckUseCase(
    private val defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
) {

    operator fun invoke(liveDecks: List<Deck>): CaptureDeckChoice {
        val candidates: List<Deck> = captureCandidates(liveDecks)
        val defaultDeckId: DeckId? = defaultDeckSelectionRepository.getDefaultDeckId()
        val target: Deck? = candidates.find { it.id == defaultDeckId } ?: candidates.minByOrNull(Deck::createdAt)
        return CaptureDeckChoice(candidates = candidates, target = target)
    }

    private fun captureCandidates(liveDecks: List<Deck>): List<Deck> {
        val userDecks: List<Deck> = liveDecks.filterNot { it.id.isInstalledCuratedDeck() }
        return userDecks.ifEmpty { liveDecks }
    }
}
