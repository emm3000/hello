package com.emm.hello.features.addword.domain

import kotlinx.coroutines.flow.Flow

interface WordRepository {

    fun fetchAll(): Flow<List<Word>>

    suspend fun upsert(word: Word)

    suspend fun deleteBy(wordId: String)

    suspend fun selectBy(wordId: String): Word?
}