package com.emm.domain.deck

class DeckCreator(private val repository: DeckRepository) {

    suspend fun create(input: CreateDeckInput) {
        repository.addDeck(input)
    }
}