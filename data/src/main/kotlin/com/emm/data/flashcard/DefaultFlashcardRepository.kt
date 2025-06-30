package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.FlashcardExampleQueries
import com.emm.data.FlashcardQueries
import com.emm.data.FlashcardWithExamples
import com.emm.data.HelloDb
import com.emm.data.SyncStatus
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardGenerated
import com.emm.domain.flashcard.FlashcardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

typealias FlashcardEntity = com.emm.data.Flashcard

class DefaultFlashcardRepository(
    private val db: HelloDb,
    private val geminiService: GeminiService,
    private val json: Json,
) : FlashcardRepository {

    private val dao: FlashcardQueries = db.flashcardQueries

    private val exampleDao: FlashcardExampleQueries = db.flashcardExampleQueries

    override suspend fun create(input: CreateFlashcardInput) = withContext(Dispatchers.IO) {
        val cardId: String = UUID.randomUUID().toString()
        dao.create(
            id = cardId,
            deckId = input.deckId,
            word = input.word,
            meaning = input.meaning,
            translation = input.translation,
            phonetic = input.phonetic,
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            syncStatus = SyncStatus.Pending.name,
        )
        return@withContext cardId
    }

    override suspend fun upsertExamples(
        examples: List<Example>,
        flashcardId: String,
    ) = withContext(Dispatchers.IO) {
        db.transaction { populate(examples, flashcardId) }
    }

    private fun populate(examples: List<Example>, flashcardId: String) {
        examples.forEach {
            exampleDao.insert(
                id = UUID.randomUUID().toString(),
                flashcardId = flashcardId,
                text = it.text,
                translation = it.translation,
                type = it.type,
                createdAt = Instant.now().toEpochMilli(),
                updatedAt = Instant.now().toEpochMilli(),
                syncStatus = SyncStatus.Synced.name,
            )
        }
    }

    override suspend fun generateFlashcard(word: String): FlashcardGenerated = withContext(Dispatchers.IO) {
        val prompt: String = Prompt.buildPrompt(word)
        val response: String = geminiService.process(prompt)
        val flashcardGenerated: FlashcardGenerated = FlashcardResponseParses.parse(response, json)
        return@withContext flashcardGenerated
    }

    override fun fetchAll(): Flow<List<Flashcard>> = dao
        .all()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map(List<FlashcardEntity>::toDomain)

    override fun fetchByDeckId(deckId: String): Flow<List<Flashcard>> = dao
        .selectByDeck(deckId)
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map(List<FlashcardEntity>::toDomain)

    override suspend fun fetchById(id: String): Flashcard = withContext(Dispatchers.IO) {
        val flashcardEntities: List<FlashcardWithExamples> = dao
            .flashcardWithExamples(id)
            .executeAsList()

        val first: FlashcardWithExamples = flashcardEntities.firstOrNull() ?: throw Exception("Flashcard not found")

        val examples: List<Example> = flashcardEntities.mapNotNull {
            if (it.exampleId != null && it.exampleText != null && it.exampleTranslation != null && it.exampleType != null) {
                return@mapNotNull Example(
                    exampleId = it.exampleId,
                    text = it.exampleText,
                    translation = it.exampleTranslation,
                    type = it.exampleType,
                )
            }
            null
        }
        Flashcard(
            id = first.id,
            word = first.word,
            meaning = first.meaning,
            translation = first.translation.orEmpty(),
            phonetic = first.phonetic.orEmpty(),
            examples = examples,
        )
    }
}