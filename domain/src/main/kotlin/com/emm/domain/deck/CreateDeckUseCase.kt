package com.emm.domain.deck

class CreateDeckUseCase(private val repository: DeckRepository) {

    suspend fun create(input: CreateDeckInput) {
        repository.addDeck(input)
    }
}
