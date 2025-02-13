package com.emm.hello.features.backup

import com.emm.data.word.WordDao
import com.emm.data.word.WordEntity
import com.emm.data.wordcontent.ExampleDao
import com.emm.data.wordcontent.ExampleEntity
import com.emm.data.wordcontent.WordContentDao
import com.emm.data.wordcontent.WordContentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json

class DataModeler(
    private val wordDao: WordDao,
    private val wordContentDao: WordContentDao,
    private val exampleDao: ExampleDao,
) {

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    suspend fun model(): String {
        val wordEntities: List<WordEntity> = wordDao.all().firstOrNull().orEmpty()
        val wordContentEntities: List<WordContentEntity> = wordContentDao.select().firstOrNull().orEmpty()
        val exampleEntities: List<ExampleEntity> = exampleDao.select().firstOrNull().orEmpty()
        val wordJsons = WordJson(
            wordEntities = wordEntities,
            wordContentEntities = wordContentEntities,
            exampleEntities = exampleEntities,
        )
        return json.encodeToString(wordJsons)
    }

    suspend fun inverse(json: String) {
        val wordJsons: WordJson = this.json.decodeFromString<WordJson>(json)
        wordDao.upsert(wordJsons.wordEntities)
        wordContentDao.upsert(wordJsons.wordContentEntities)
        exampleDao.upsert(wordJsons.exampleEntities)
    }

    @OptIn(FlowPreview::class)
    fun observeAll(): Flow<String> = combine(
        wordDao.all().distinctUntilChanged(),
        wordContentDao.select().distinctUntilChanged(),
        exampleDao.select().distinctUntilChanged(),
    ) { first, second, third ->
        """
            wordChange: ${first.size} 
            contentChange: ${second.size} 
            exampleChange: ${third.size}
        """.trimIndent()
    }
        .debounce(10_000L)
        .flowOn(Dispatchers.IO)
}