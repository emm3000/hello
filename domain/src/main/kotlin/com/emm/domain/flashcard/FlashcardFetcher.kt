package com.emm.domain.flashcard

import kotlinx.coroutines.flow.Flow

class FlashcardFetcher(private val repository: FlashcardRepository) {

    fun fetchAll(): Flow<List<Flashcard>> {
        return repository.fetchAll()
    }
}