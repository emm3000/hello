package com.emm.hello.features.addword.domain

import com.emm.data.WordDao
import com.emm.data.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalWordRepository(private val wordDao: WordDao): WordRepository {

    override fun fetchAll(): Flow<List<Word>> {
        return wordDao.all().map(::toDomain).flowOn(Dispatchers.IO)
    }

    private fun toDomain(wordEntities: List<WordEntity>) = wordEntities.map { wordEntity ->
        Word(
            id = wordEntity.id,
            word = wordEntity.word,
            createdAt = wordEntity.createdAt
        )
    }

    override suspend fun upsert(word: Word) = withContext(Dispatchers.IO) {
        val wordEntity = WordEntity(
            id = word.id,
            word = word.word,
            createdAt = word.createdAt,
        )
        wordDao.insert(wordEntity)
    }

    override suspend fun deleteBy(wordId: String) = withContext(Dispatchers.IO) {
        val wordEntity: WordEntity = wordDao.selectBy(wordId) ?: return@withContext
        wordDao.delete(wordEntity)
    }
}