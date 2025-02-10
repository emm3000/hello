package com.emm.domain

class WordContentFetcher(private val repository: WordContentRepository) {

    suspend fun fetch(wordId: String): WordContent? {
        return repository.fetchContentBy(wordId)
    }
}