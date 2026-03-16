package com.emm.domain.deck

class CreateDeckUseCase(private val repository: DeckRepository) {

    suspend operator fun invoke(input: CreateDeckInput) {
        repository.addDeck(input)
    }
}
