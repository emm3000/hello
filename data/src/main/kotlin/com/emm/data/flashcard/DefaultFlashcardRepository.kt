package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.FlashcardExampleQueries
import com.emm.data.FlashcardQueries
import com.emm.data.FlashcardWithExamples
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@LocalFirstWrite
class DefaultFlashcardRepository(
    private val db: HelloDb,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) : FlashcardRepository {

    private val dao: FlashcardQueries = db.flashcardQueries

    private val exampleDao: FlashcardExampleQueries = db.flashcardExampleQueries

    private val artifactEncoder = LearningNoteArtifactEncoder(json)

    override suspend fun create(input: CreateFlashcardInput) = withContext(ioDispatcher) {
        val cardId: FlashcardId = input.id ?: UUID.randomUUID().toString().toFlashcardId()
        val now: Long = Instant.now().toEpochMilli()
        val artifacts = encodeArtifacts(input)

        db.transaction {
            dao.create(
                id = cardId.value,
                deckId = input.deckId.value,
                word = input.word,
                meaning = input.meaning,
                translation = input.translation,
                phonetic = input.phonetic,
                partOfSpeech = input.partOfSpeech,
                type = input.noteType,
                note = input.noteSummary,
                register = input.register,
                levelBand = input.levelBand,
                domain = input.learningDomain,
                lemma = input.lemma,
                whyUseful = input.whyUseful,
                usagePattern = input.usagePattern,
                irregularFormsJson = artifacts.irregularFormsJson,
                collocationsJson = artifacts.collocationsJson,
                commonMistake = input.commonMistake,
                confusableWithJson = artifacts.confusableWithJson,
                clozeSentence = input.clozeSentence,
                sourceContext = input.sourceContext,
                warningsJson = artifacts.warningsJson,
                studyCardsJson = artifacts.studyCardsJson,
                qualityChecksJson = artifacts.qualityChecksJson,
                enrichmentStatus = input.enrichmentStatus.name,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
        }
        return@withContext cardId
    }

    private fun encodeArtifacts(input: CreateFlashcardInput): EncodedFlashcardArtifacts {
        return EncodedFlashcardArtifacts(
            irregularFormsJson = json.encodeToString(input.irregularForms),
            collocationsJson = json.encodeToString(input.collocations),
            confusableWithJson = json.encodeToString(input.confusableWith),
            warningsJson = json.encodeToString(input.warnings),
            studyCardsJson = artifactEncoder.encodeStudyCards(input.studyCards),
            qualityChecksJson = artifactEncoder.encodeQualityChecks(input.qualityChecks),
        )
    }

    override suspend fun upsertExamples(
        examples: List<Example>,
        flashcardId: FlashcardId,
    ) = withContext(ioDispatcher) {
        db.transaction { populate(examples, flashcardId.value) }
    }

    override suspend fun update(input: UpdateFlashcardInput) = withContext(ioDispatcher) {
        require(input.word.isNotBlank()) { "Flashcard word must not be blank." }
        require(input.meaning.isNotBlank()) { "Flashcard meaning must not be blank." }

        val now: Long = Instant.now().toEpochMilli()
        val cardId: String = input.flashcardId.value

        db.transactionWithResult {
            // Verify the flashcard exists and is not deleted
            val existing = dao.findById(cardId).executeAsOneOrNull()
                ?: throw NoSuchElementException("Flashcard not found: $cardId")
            check(existing.deletedAt == null) { "Flashcard already deleted: $cardId" }

            // Update the flashcard row
            dao.update(
                word = input.word,
                meaning = input.meaning,
                translation = input.translation,
                phonetic = input.phonetic,
                partOfSpeech = input.partOfSpeech,
                type = input.noteType,
                note = input.noteSummary,
                register = input.register,
                levelBand = input.levelBand,
                domain = input.learningDomain,
                lemma = input.lemma,
                whyUseful = input.whyUseful,
                usagePattern = input.usagePattern,
                irregularFormsJson = json.encodeToString(input.irregularForms),
                collocationsJson = json.encodeToString(input.collocations),
                commonMistake = input.commonMistake,
                confusableWithJson = json.encodeToString(input.confusableWith),
                clozeSentence = input.clozeSentence,
                sourceContext = input.sourceContext,
                warningsJson = json.encodeToString(input.warnings),
                updatedAt = now,
                id = cardId,
            )

            syncExamples(cardId = cardId, now = now, input = input)

            Unit
        }
    }

    override suspend fun updateEnrichmentStatus(
        flashcardId: FlashcardId,
        status: EnrichmentStatus,
    ): Unit = withContext(ioDispatcher) {
        dao.setEnrichmentStatus(
            enrichmentStatus = status.name,
            updatedAt = Instant.now().toEpochMilli(),
            id = flashcardId.value,
        )
    }

    override suspend fun countDueFlashcards(nowMillis: Long): Long = withContext(ioDispatcher) {
        dao.countDueFlashcards(now = nowMillis).executeAsOne()
    }

    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = withContext(ioDispatcher) {
        val now: Long = Instant.now().toEpochMilli()

        db.transactionWithResult {
            // Verify the flashcard exists and is not already deleted
            val existing = dao.findById(flashcardId.value).executeAsOneOrNull()
                ?: throw NoSuchElementException("Flashcard not found: ${flashcardId.value}")
            check(existing.deletedAt == null) { "Flashcard already deleted: ${flashcardId.value}" }

            // Cascade: flashcard → examples
            dao.softDelete(now = now, id = flashcardId.value)
            dao.softDeleteExamplesByFlashcard(now = now, flashcardId = flashcardId.value)

            now
        }
    }

    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = withContext(ioDispatcher) {
        db.transaction {
            // Restore only rows stamped with the exact cascade timestamp.
            dao.restoreExamplesByFlashcard(flashcardId = flashcardId.value, deletedAt = deletedAt)
            dao.restoreFlashcard(id = flashcardId.value, deletedAt = deletedAt)
        }
    }

    private fun syncExamples(
        cardId: String,
        now: Long,
        input: UpdateFlashcardInput,
    ) {
        val existingExamples = exampleDao.findByFlashcardId(cardId).executeAsList()
            .filter { it.deletedAt == null }
        val existingIds = existingExamples.map { it.id }.toSet()

        val inputIds = input.examples.map { it.exampleId }.filter { it.isNotBlank() }.toSet()

        existingExamples.forEach { existing ->
            if (existing.id !in inputIds) {
                exampleDao.softDelete(now = now, id = existing.id)
            }
        }

        input.examples.forEach { example ->
            val exampleId = example.exampleId.ifBlank { UUID.randomUUID().toString() }
            val text = example.text
            val translation = example.translation
            val type = example.type

            if (exampleId in existingIds) {
                exampleDao.insert(
                    id = exampleId,
                    flashcardId = cardId,
                    text = text,
                    translation = translation,
                    type = type,
                    createdAt = existingExamples.first { it.id == exampleId }.createdAt,
                    updatedAt = now,
                    deletedAt = null,
                )
            } else {
                exampleDao.insert(
                    id = exampleId,
                    flashcardId = cardId,
                    text = text,
                    translation = translation,
                    type = type,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                )
            }
        }
    }

    private fun populate(examples: List<Example>, flashcardId: String) {
        examples.forEach {
            val now = Instant.now().toEpochMilli()
            val exampleId = UUID.randomUUID().toString()
            exampleDao.insert(
                id = exampleId,
                flashcardId = flashcardId,
                text = it.text,
                translation = it.translation,
                type = it.type,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
        }
    }

    override fun fetchAll(): Flow<List<Flashcard>> {
        return dao
            .all()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { entities -> entities.map { toDomainSummary(it, json) } }
    }

    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> {
        return dao
            .selectByDeck(deckId.value)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { entities -> entities.map { toDomainSummary(it, json) } }
    }

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail = withContext(ioDispatcher) {
        val flashcardEntities: List<FlashcardWithExamples> = dao
            .flashcardWithExamples(id.value)
            .executeAsList()

        val first: FlashcardWithExamples = flashcardEntities.firstOrNull()
            ?: throw NoSuchElementException("Flashcard not found")

        val examples: List<Example> = flashcardEntities.mapNotNull(::toExampleOrNull)
        val review = mapFsrsCard(first)
        val flashcard = toDomainDetail(first, examples, json, review)
        FlashcardDetail(
            flashcard = flashcard,
            studyCards = decodeStudyCards(first.studyCardsJson, json),
            qualityChecks = decodeQualityChecks(first.qualityChecksJson, json),
        )
    }
}

private data class EncodedFlashcardArtifacts(
    val irregularFormsJson: String,
    val collocationsJson: String,
    val confusableWithJson: String,
    val warningsJson: String,
    val studyCardsJson: String,
    val qualityChecksJson: String,
)
