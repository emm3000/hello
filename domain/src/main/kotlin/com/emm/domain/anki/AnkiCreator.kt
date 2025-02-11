package com.emm.domain.anki

class AnkiCreator(private val repository: AnkiRepository) {

    suspend fun create(input: String): Anki {
        return repository.createCard(input)
    }
}