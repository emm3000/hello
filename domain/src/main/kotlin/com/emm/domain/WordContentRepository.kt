package com.emm.domain

interface WordContentRepository {

    suspend fun createThenSave(word: Word)

    suspend fun fetchContent(word: String): WordContent?
}