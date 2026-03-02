package com.emm.domain.deprecated.word

import kotlinx.coroutines.flow.Flow

interface WordRepository {

    fun fetchAll(): Flow<List<Word>>

    suspend fun upsert(word: Word)

    suspend fun deleteBy(wordId: String)

    suspend fun selectBy(wordId: String): Word?

    suspend fun updateHasContent(word: Word, hasContent: Boolean)
}
