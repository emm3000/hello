package com.emm.hello.features.backup

import com.emm.data.word.WordDao
import com.emm.data.word.WordEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

class DataModeler(private val wordDao: WordDao) {

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    suspend fun model(): String {
        val wordEntities: List<WordEntity> = wordDao.all().firstOrNull().orEmpty()
        val wordJsons: List<WordJson> = wordEntities.map { WordJson(it.id, it.word, it.createdAt, it.updatedAt) }
        return json.encodeToString(wordJsons)
    }

    suspend fun inverse(json: String) {
        val wordJsons: List<WordJson> = this.json.decodeFromString<List<WordJson>>(json)
        val wordEntities = wordJsons.map {
            WordEntity(
                id = it.id,
                word = it.word,
                hasContent = false,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        wordDao.upsert(wordEntities)
    }
}