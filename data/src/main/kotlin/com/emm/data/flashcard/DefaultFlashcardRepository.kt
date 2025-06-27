package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.FlashcardQueries
import com.emm.data.HelloDb
import com.emm.data.flashcard.dto.FlashcardDto
import com.emm.data.flashcard.dto.ResponseError
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.util.UUID

typealias FlashcardEntity = com.emm.data.Flashcard

class DefaultFlashcardRepository(
    db: HelloDb,
    private val geminiService: GeminiService,
    private val json: Json,
) : FlashcardRepository {

    private val dao: FlashcardQueries = db.flashcardQueries

    override suspend fun create(word: String, deckId: String) = withContext(Dispatchers.IO) {
        val buildPrompt: String = Prompt.buildPrompt(word)
        val geminiResponse: String = geminiService.process(buildPrompt)
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root: JsonObject = json.parseToJsonElement(geminiResponse).jsonObject
        val isSuccess: Boolean = root["success"]?.jsonPrimitive?.booleanOrNull == true

        if (isSuccess) {
            val data: FlashcardDto = json.decodeFromString<FlashcardDto>(root["data"]?.jsonObject.toString())
            dao.create(
                id = UUID.randomUUID().toString(),
                deckId = deckId,
                word = data.word,
                meaning = data.meaning,
                translation = data.translation,
                example = data.example,
                phonetic = data.phonetic,
                createdAt = Instant.now().toEpochMilli(),
            )
            root["data"]?.jsonObject.toString()
        } else {
            val data: ResponseError = json.decodeFromString<ResponseError>(root["error"]?.jsonObject.toString())
            throw IllegalArgumentException("${data.message} -> ${data.input}")
        }
    }

    override fun fetchAll(): Flow<List<Flashcard>> = dao
        .all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map(List<FlashcardEntity>::toDomain)
}