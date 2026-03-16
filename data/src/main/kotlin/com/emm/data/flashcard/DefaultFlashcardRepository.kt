package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.FlashcardExampleQueries
import com.emm.data.FlashcardQueries
import com.emm.data.FlashcardWithExamples
import com.emm.data.FlashcardsToReviewByDeck
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.data.localfirst.OperationLogWriter
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardGenerated
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardReadRepository
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardWriteRepository
import com.emm.domain.flashcard.StaticCategories
import com.emm.domain.flashcard.StudySessionRepository
import com.emm.domain.sync.OperationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

@LocalFirstWrite
class DefaultFlashcardRepository(
    private val db: HelloDb,
    private val geminiService: GeminiService,
    private val json: Json,
    private val operationLogWriter: OperationLogWriter,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
) : FlashcardReadRepository, FlashcardWriteRepository, StudySessionRepository, FlashcardGenerationRepository {

    private val dao: FlashcardQueries = db.flashcardQueries

    private val exampleDao: FlashcardExampleQueries = db.flashcardExampleQueries

    override suspend fun create(input: CreateFlashcardInput) = withContext(Dispatchers.IO) {
        val cardId: String = input.id ?: UUID.randomUUID().toString()
        val now: Long = Instant.now().toEpochMilli()
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()

        db.transaction {
            val payloadJson = buildJsonObject {
                put("entityId", cardId)
                put("operationType", OperationType.Create.name)
                put("deckId", input.deckId)
                put("word", input.word)
                put("meaning", input.meaning)
            }.toString()
            val lamport = operationLogWriter.appendOperation(
                entityType = "flashcard",
                entityId = cardId,
                operationType = OperationType.Create,
                payloadJson = payloadJson,
                originDeviceId = deviceId,
                createdAt = now,
            )
            dao.create(
                id = cardId,
                deckId = input.deckId,
                word = input.word,
                meaning = input.meaning,
                translation = input.translation,
                phonetic = input.phonetic,
                partOfSpeech = input.partOfSpeech,
                type = input.type,
                note = input.note,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                originDeviceId = deviceId,
                lastModifiedByDeviceId = deviceId,
                versionLamport = lamport,
            )
        }
        return@withContext cardId
    }

    override suspend fun upsertExamples(
        examples: List<Example>,
        flashcardId: String,
    ) = withContext(Dispatchers.IO) {
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
        db.transaction { populate(examples, flashcardId, deviceId) }
    }

    private fun populate(examples: List<Example>, flashcardId: String, deviceId: String) {
        examples.forEach {
            val now = Instant.now().toEpochMilli()
            val exampleId = UUID.randomUUID().toString()
            val payloadJson = buildJsonObject {
                put("entityId", exampleId)
                put("operationType", OperationType.Create.name)
                put("flashcardId", flashcardId)
                put("text", it.text)
                put("translation", it.translation)
                put("type", it.type)
            }.toString()
            val lamport = operationLogWriter.appendOperation(
                entityType = "flashcard_example",
                entityId = exampleId,
                operationType = OperationType.Create,
                payloadJson = payloadJson,
                originDeviceId = deviceId,
                createdAt = now,
            )
            exampleDao.insert(
                id = exampleId,
                flashcardId = flashcardId,
                text = it.text,
                translation = it.translation,
                type = it.type,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                originDeviceId = deviceId,
                lastModifiedByDeviceId = deviceId,
                versionLamport = lamport,
            )
        }
    }

    override suspend fun generateFlashcard(word: String): FlashcardGenerated = withContext(Dispatchers.IO) {
        val prompt: String = Prompt.buildPrompt(word)
        val response: String = geminiService.process(prompt)
        val flashcardGenerated: FlashcardGenerated = FlashcardResponseParses.parse(response, json)
        return@withContext flashcardGenerated
    }

    override suspend fun generatedFlashcard(
        categories: StaticCategories,
        difficulty: String
    ): FlashcardGenerated = withContext(Dispatchers.IO) {
        val prompt: String = Prompt.buildPrompt(categories.name, difficulty)
        val response: String = geminiService.process(prompt)
        val flashcardGenerated: FlashcardGenerated = AnkiResponseParses.parse(response, json)
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

        val first: FlashcardWithExamples = flashcardEntities.firstOrNull()
            ?: throw NoSuchElementException("Flashcard not found")

        val examples: List<Example> = flashcardEntities.mapNotNull(::toExampleOrNull)
        Flashcard(
            id = first.id,
            word = first.word,
            meaning = first.meaning,
            translation = first.translation.orEmpty(),
            phonetic = first.phonetic.orEmpty(),
            examples = examples,
            review = FlashcardReview.Empty,
            partOfSpeech = first.partOfSpeech.orEmpty(),
            type = first.type.orEmpty(),
            note = first.note.orEmpty(),
        )
    }

    override suspend fun sessionToday(deckId: String): List<Flashcard> = withContext(Dispatchers.IO) {
        val flashcardsToReviewByDeck: List<FlashcardsToReviewByDeck> = dao.flashcardsToReviewByDeck(
            deckId = deckId,
            now = Instant.now().toEpochMilli(),
        ).executeAsList()

        flashcardsToReviewByDeck.map {
            val review: FlashcardReview = mapFlashcardReview(it)
            Flashcard(
                id = it.id,
                word = it.word,
                meaning = it.meaning,
                translation = it.translation.orEmpty(),
                phonetic = it.phonetic.orEmpty(),
                examples = emptyList(),
                review = review,
                partOfSpeech = it.partOfSpeech.orEmpty(),
                type = it.type.orEmpty(),
                note = it.note.orEmpty(),
            )
        }
    }

    override fun flashcardWithReview(deckId: String): Flow<List<Flashcard>> {
        return dao.flashcardsWithReview(deckId).asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    Flashcard(
                        id = it.id,
                        word = it.word,
                        meaning = it.meaning,
                        translation = it.translation.orEmpty(),
                        phonetic = it.phonetic.orEmpty(),
                        examples = emptyList(),
                        review = FlashcardReview.Empty.copy(
                            nextReviewAt = it.nextReviewAt ?: Instant.now().toEpochMilli()
                        ),
                        partOfSpeech = it.partOfSpeech.orEmpty(),
                        type = it.type.orEmpty(),
                        note = it.note.orEmpty(),
                    )
                }
            }
    }

    private fun toExampleOrNull(item: FlashcardWithExamples): Example? {
        val hasMissingField = listOf(
            item.exampleId,
            item.exampleText,
            item.exampleTranslation,
            item.exampleType,
        ).any { it == null }

        if (hasMissingField) return null

        return Example(
            exampleId = item.exampleId.orEmpty(),
            text = item.exampleText.orEmpty(),
            translation = item.exampleTranslation.orEmpty(),
            type = item.exampleType.orEmpty(),
        )
    }

    private fun mapFlashcardReview(deck: FlashcardsToReviewByDeck): FlashcardReview {
        val hasMissingField = listOf(
            deck.flashcardId,
            deck.lastReviewedAt,
            deck.nextReviewAt,
            deck.easeFactor,
            deck.interval,
            deck.repetitions,
            deck.lapses,
        ).any { it == null }

        if (hasMissingField) return FlashcardReview.Empty

        return FlashcardReview(
            flashcardId = deck.flashcardId ?: "",
            lastReviewedAt = deck.lastReviewedAt ?: 0L,
            nextReviewAt = deck.nextReviewAt ?: 0L,
            easeFactor = deck.easeFactor ?: 0.0,
            interval = deck.interval ?: 0L,
            repetitions = deck.repetitions ?: 0L,
            lapses = deck.lapses ?: 0L,
        )
    }
}
