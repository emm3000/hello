package com.emm.hello.page

import com.emm.data.WordDao
import com.emm.data.WordEntity
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
        val wordJsons: List<WordJson> = wordEntities.map { WordJson(it.id, it.word, it.date) }
        return json.encodeToString(wordJsons)
    }
}