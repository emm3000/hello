package com.emm.domain

interface WordContentRepository {

    suspend fun createScrappingContent(word: Word): WordContent

    suspend fun createIAContent(word: Word): WordContent

    suspend fun saveContent(wordContent: WordContent, wordId: String)

    suspend fun fetchContentBy(wordId: String): List<WordContent>
}