package com.emm.data.word

import com.emm.domain.Word
import com.emm.domain.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalWordRepository(private val wordDao: WordDao) : WordRepository {

    override fun fetchAll(): Flow<List<Word>> {
        return wordDao.all().map(::toDomain).flowOn(Dispatchers.IO)
    }

    private fun toDomain(wordEntities: List<WordEntity>) = wordEntities.map { wordEntity ->
        Word(
            id = wordEntity.id,
            word = wordEntity.word,
            hasContent = wordEntity.hasContent,
            createdAt = wordEntity.createdAt,
        )
    }

    override suspend fun upsert(word: Word) = withContext(Dispatchers.IO) {
        val wordEntity = WordEntity(
            id = word.id,
            word = word.word,
            hasContent = word.hasContent,
            createdAt = word.createdAt,
        )
        wordDao.insert(wordEntity)
    }

    override suspend fun deleteBy(wordId: String) = withContext(Dispatchers.IO) {
        val wordEntity: WordEntity = wordDao.selectBy(wordId) ?: return@withContext
        wordDao.delete(wordEntity)
    }

    override suspend fun selectBy(wordId: String): Word? = withContext(Dispatchers.IO) {
        val wordEntity: WordEntity = wordDao.selectBy(wordId) ?: return@withContext null
        return@withContext Word(
            id = wordEntity.id,
            word = wordEntity.word,
            hasContent = wordEntity.hasContent,
            createdAt = wordEntity.createdAt,
        )
    }
}