package com.emm.domain

interface WordContentRepository {

    suspend fun create(word: Word)

    suspend fun save(content: WordContent)
}