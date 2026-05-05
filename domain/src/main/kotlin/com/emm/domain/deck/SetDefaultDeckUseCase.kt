package com.emm.domain.deck

import com.emm.domain.ids.DeckId

class SetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    operator fun invoke(deckId: DeckId?) {
        repository.setDefaultDeckId(deckId)
    }
}
