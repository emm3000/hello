package com.emm.domain.deck

class SetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    operator fun invoke(deckId: String) {
        repository.setDefaultDeckId(deckId)
    }
}
