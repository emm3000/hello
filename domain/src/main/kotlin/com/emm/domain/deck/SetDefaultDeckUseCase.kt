package com.emm.domain.deck

class SetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    fun execute(deckId: String) {
        repository.setDefaultDeckId(deckId)
    }
}
