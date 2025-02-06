package com.emm.domain

class WordContentCreator(private val repository: WordContentRepository) {

    suspend fun create(word: Word) {
        repository.createThenSave(word)
    }
}