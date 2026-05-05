package com.emm.domain.deck

import com.emm.domain.ids.toDeckId

class SetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    operator fun invoke(deckId: String) {
        val typedDeckId = deckId.toDeckId()
        repository.setDefaultDeckId(typedDeckId)
    }
}
