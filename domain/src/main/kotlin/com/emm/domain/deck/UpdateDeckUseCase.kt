package com.emm.domain.deck

class UpdateDeckUseCase(
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke(input: UpdateDeckInput) {
        require(input.name.isNotBlank()) { "Deck name must not be blank." }

        deckRepository.updateDeck(input)
    }
}