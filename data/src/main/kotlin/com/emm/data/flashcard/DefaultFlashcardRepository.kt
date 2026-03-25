@file:Suppress("ImportOrdering")

package com.emm.data.flashcard

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.FlashcardExampleQueries
import com.emm.data.FlashcardQueries
import com.emm.data.FlashcardWithExamples
import com.emm.data.FlashcardsToReviewByDeck
import com.emm.data.HelloDb
import com.emm.data.flashcard.iadto.StoredNoteQualityCheckDto
import com.emm.data.flashcard.iadto.StoredStudyCardDto
import com.emm.data.localfirst.requireCurrentAppAccountId
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.data.localfirst.OperationLogWriter
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardReadRepository
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.FlashcardWriteRepository
import com.emm.domain.flashcard.GeneratedExampleDraft
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.GeneratedNoteQualityCheck
import com.emm.domain.flashcard.GeneratedNoteQualityCode
import com.emm.domain.flashcard.GeneratedStudyCard
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.flashcard.StudySessionRepository
import com.emm.domain.sync.OperationType
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
        val appAccountId = db.requireCurrentAppAccountId()
        val artifacts = encodeArtifacts(input)

        db.transaction {
            val payloadJson = buildCreatePayloadJson(cardId, input, artifacts, now)
            val lamport = operationLogWriter.appendOperation(
                entityType = "flashcard",
                entityId = cardId,
                operationType = OperationType.Create,
                payloadJson = payloadJson,
                originDeviceId = deviceId,
                createdAt = now,
            )
            dao.create(
                appAccountId = appAccountId,
                id = cardId,
                deckId = input.deckId,
                word = input.word,
                meaning = input.meaning,
                translation = input.translation,
                phonetic = input.phonetic,
                partOfSpeech = input.partOfSpeech,
                type = input.type,
                note = input.note,
                register = input.register,
                levelBand = input.levelBand,
                domain = input.domain,
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

    private fun buildCreatePayloadJson(
        cardId: String,
        input: CreateFlashcardInput,
        artifacts: EncodedFlashcardArtifacts,
        now: Long,
    ): String {
        return buildJsonObject {
            put("entityId", cardId)
            put("operationType", OperationType.Create.name)
            put("deckId", input.deckId)
            put("word", input.word)
            put("meaning", input.meaning)
            put("translation", input.translation)
            put("phonetic", input.phonetic)
            put("partOfSpeech", input.partOfSpeech)
            put("type", input.type)
            put("note", input.note)
            put("register", input.register)
            put("levelBand", input.levelBand)
            put("domain", input.domain)
            put("lemma", input.lemma)
            put("whyUseful", input.whyUseful)
            put("usagePattern", input.usagePattern)
            put("irregularFormsJson", artifacts.irregularFormsJson)
            put("collocationsJson", artifacts.collocationsJson)
            put("commonMistake", input.commonMistake)
            put("confusableWithJson", artifacts.confusableWithJson)
            put("clozeSentence", input.clozeSentence)
            put("sourceContext", input.sourceContext)
            put("warningsJson", artifacts.warningsJson)
            put("studyCardsJson", artifacts.studyCardsJson)
            put("qualityChecksJson", artifacts.qualityChecksJson)
            put("createdAt", now)
            put("updatedAt", now)
        }.toString()
    }

    private fun encodeArtifacts(input: CreateFlashcardInput): EncodedFlashcardArtifacts {
        return EncodedFlashcardArtifacts(
            irregularFormsJson = json.encodeToString(input.irregularForms),
            collocationsJson = json.encodeToString(input.collocations),
            confusableWithJson = json.encodeToString(input.confusableWith),
            warningsJson = json.encodeToString(input.warnings),
            studyCardsJson = json.encodeToString(input.studyCards.toStoredStudyCardDtos()),
            qualityChecksJson = json.encodeToString(input.qualityChecks.toStoredQualityCheckDtos()),
        )
    }

    override suspend fun upsertExamples(
        examples: List<Example>,
        flashcardId: String,
    ) = withContext(Dispatchers.IO) {
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
        val appAccountId = db.requireCurrentAppAccountId()
        db.transaction { populate(examples, flashcardId, deviceId, appAccountId) }
    }

    private fun populate(examples: List<Example>, flashcardId: String, deviceId: String, appAccountId: String) {
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
                appAccountId = appAccountId,
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

    override suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote =
        withContext(Dispatchers.IO) {
            val prompt = Prompt.buildLearningNotePrompt(input)
            val response = geminiService.process(prompt)
            GeneratedLearningNoteResponseParser.parse(response, json)
        }

    override suspend fun regenerateExample(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): GeneratedExampleDraft = withContext(Dispatchers.IO) {
        val prompt = Prompt.buildExampleRegenerationPrompt(input, note)
        val response = geminiService.process(prompt)
        PartialRegenerationParser.parseExample(response, json)
    }

    override suspend fun regenerateClozeSentence(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
    ): String = withContext(Dispatchers.IO) {
        val prompt = Prompt.buildClozeRegenerationPrompt(input, note)
        val response = geminiService.process(prompt)
        PartialRegenerationParser.parseCloze(response, json)
    }

    override suspend fun regenerateStudyCard(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        cardId: String,
    ): GeneratedStudyCard = withContext(Dispatchers.IO) {
        val card = note.cards.firstOrNull { it.cardId == cardId }
            ?: throw IllegalArgumentException("Study card no encontrada para regenerar.")
        val prompt = Prompt.buildStudyCardRegenerationPrompt(input, note, card)
        val response = geminiService.process(prompt)
        PartialRegenerationParser.parseStudyCard(response, json)
    }

    override fun fetchAll(): Flow<List<Flashcard>> {
        val appAccountId = db.requireCurrentAppAccountId()
        return dao
            .all(appAccountId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map(::toDomainSummary) }
    }

    override fun fetchByDeckId(deckId: String): Flow<List<Flashcard>> {
        val appAccountId = db.requireCurrentAppAccountId()
        return dao
            .selectByDeck(appAccountId, deckId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map(::toDomainSummary) }
    }

    override suspend fun fetchById(id: String): Flashcard = withContext(Dispatchers.IO) {
        val appAccountId = db.requireCurrentAppAccountId()
        val flashcardEntities: List<FlashcardWithExamples> = dao
            .flashcardWithExamples(appAccountId, id)
            .executeAsList()

        val first: FlashcardWithExamples = flashcardEntities.firstOrNull()
            ?: throw NoSuchElementException("Flashcard not found")

        val examples: List<Example> = flashcardEntities.mapNotNull(::toExampleOrNull)
        toDomainDetail(first, examples)
    }

    override suspend fun sessionToday(deckId: String): List<Flashcard> = withContext(Dispatchers.IO) {
        val appAccountId = db.requireCurrentAppAccountId()
        val flashcardsToReviewByDeck: List<FlashcardsToReviewByDeck> = dao.flashcardsToReviewByDeck(
            appAccountId = appAccountId,
            deckId = deckId,
            now = Instant.now().toEpochMilli(),
        ).executeAsList()

        flashcardsToReviewByDeck.map {
            val review: FlashcardReview = mapFlashcardReview(it)
            toDomainSummary(it, review)
        }
    }

    override fun flashcardWithReview(deckId: String): Flow<List<Flashcard>> {
        val appAccountId = db.requireCurrentAppAccountId()
        return dao.flashcardsWithReview(appAccountId, deckId).asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    toDomainSummary(
                        entity = it,
                        review = FlashcardReview.Empty.copy(
                            nextReviewAt = it.nextReviewAt ?: Instant.now().toEpochMilli()
                        ),
                    )
                }
            }
    }

    private fun toDomainSummary(
        entity: FlashcardEntity,
        review: FlashcardReview = FlashcardReview.Empty,
    ): Flashcard {
        return Flashcard(
            id = entity.id,
            word = entity.word,
            meaning = entity.meaning,
            translation = entity.translation.orEmpty(),
            phonetic = entity.phonetic.orEmpty(),
            examples = emptyList(),
            review = review,
            partOfSpeech = entity.partOfSpeech.orEmpty(),
            type = entity.type.orEmpty(),
            note = entity.note.orEmpty(),
            register = entity.register.orEmpty(),
            levelBand = entity.levelBand.orEmpty(),
            domain = entity.domain.orEmpty(),
            lemma = entity.lemma.orEmpty(),
            whyUseful = entity.whyUseful.orEmpty(),
            usagePattern = entity.usagePattern.orEmpty(),
            irregularForms = decodeStringList(entity.irregularFormsJson),
            collocations = decodeStringList(entity.collocationsJson),
            commonMistake = entity.commonMistake.orEmpty(),
            confusableWith = decodeStringList(entity.confusableWithJson),
            clozeSentence = entity.clozeSentence.orEmpty(),
            sourceContext = entity.sourceContext.orEmpty(),
            warnings = decodeStringList(entity.warningsJson),
            studyCards = decodeStudyCards(entity.studyCardsJson),
            qualityChecks = decodeQualityChecks(entity.qualityChecksJson),
        )
    }

    private fun toDomainSummary(
        entity: FlashcardsToReviewByDeck,
        review: FlashcardReview,
    ): Flashcard {
        return Flashcard(
            id = entity.id,
            word = entity.word,
            meaning = entity.meaning,
            translation = entity.translation.orEmpty(),
            phonetic = entity.phonetic.orEmpty(),
            examples = emptyList(),
            review = review,
            partOfSpeech = entity.partOfSpeech.orEmpty(),
            type = entity.type.orEmpty(),
            note = entity.note.orEmpty(),
            register = entity.register.orEmpty(),
            levelBand = entity.levelBand.orEmpty(),
            domain = entity.domain.orEmpty(),
            lemma = entity.lemma.orEmpty(),
            whyUseful = entity.whyUseful.orEmpty(),
            usagePattern = entity.usagePattern.orEmpty(),
            irregularForms = decodeStringList(entity.irregularFormsJson),
            collocations = decodeStringList(entity.collocationsJson),
            commonMistake = entity.commonMistake.orEmpty(),
            confusableWith = decodeStringList(entity.confusableWithJson),
            clozeSentence = entity.clozeSentence.orEmpty(),
            sourceContext = entity.sourceContext.orEmpty(),
            warnings = decodeStringList(entity.warningsJson),
            studyCards = decodeStudyCards(entity.studyCardsJson),
            qualityChecks = decodeQualityChecks(entity.qualityChecksJson),
        )
    }

    private fun toDomainSummary(
        entity: com.emm.data.FlashcardsWithReview,
        review: FlashcardReview,
    ): Flashcard {
        return Flashcard(
            id = entity.id,
            word = entity.word,
            meaning = entity.meaning,
            translation = entity.translation.orEmpty(),
            phonetic = entity.phonetic.orEmpty(),
            examples = emptyList(),
            review = review,
            partOfSpeech = entity.partOfSpeech.orEmpty(),
            type = entity.type.orEmpty(),
            note = entity.note.orEmpty(),
            register = entity.register.orEmpty(),
            levelBand = entity.levelBand.orEmpty(),
            domain = entity.domain.orEmpty(),
            lemma = entity.lemma.orEmpty(),
            whyUseful = entity.whyUseful.orEmpty(),
            usagePattern = entity.usagePattern.orEmpty(),
            irregularForms = decodeStringList(entity.irregularFormsJson),
            collocations = decodeStringList(entity.collocationsJson),
            commonMistake = entity.commonMistake.orEmpty(),
            confusableWith = decodeStringList(entity.confusableWithJson),
            clozeSentence = entity.clozeSentence.orEmpty(),
            sourceContext = entity.sourceContext.orEmpty(),
            warnings = decodeStringList(entity.warningsJson),
            studyCards = decodeStudyCards(entity.studyCardsJson),
            qualityChecks = decodeQualityChecks(entity.qualityChecksJson),
        )
    }

    private fun toDomainDetail(
        entity: FlashcardWithExamples,
        examples: List<Example>,
    ): Flashcard {
        return Flashcard(
            id = entity.id,
            word = entity.word,
            meaning = entity.meaning,
            translation = entity.translation.orEmpty(),
            phonetic = entity.phonetic.orEmpty(),
            examples = examples,
            review = FlashcardReview.Empty,
            partOfSpeech = entity.partOfSpeech.orEmpty(),
            type = entity.type.orEmpty(),
            note = entity.note.orEmpty(),
            register = entity.register.orEmpty(),
            levelBand = entity.levelBand.orEmpty(),
            domain = entity.domain.orEmpty(),
            lemma = entity.lemma.orEmpty(),
            whyUseful = entity.whyUseful.orEmpty(),
            usagePattern = entity.usagePattern.orEmpty(),
            irregularForms = decodeStringList(entity.irregularFormsJson),
            collocations = decodeStringList(entity.collocationsJson),
            commonMistake = entity.commonMistake.orEmpty(),
            confusableWith = decodeStringList(entity.confusableWithJson),
            clozeSentence = entity.clozeSentence.orEmpty(),
            sourceContext = entity.sourceContext.orEmpty(),
            warnings = decodeStringList(entity.warningsJson),
            studyCards = decodeStudyCards(entity.studyCardsJson),
            qualityChecks = decodeQualityChecks(entity.qualityChecksJson),
        )
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    private fun decodeStudyCards(raw: String?): List<GeneratedStudyCard> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredStudyCardDto>>(raw).map { dto -> dto.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun decodeQualityChecks(raw: String?): List<GeneratedNoteQualityCheck> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredNoteQualityCheckDto>>(raw).map { dto -> dto.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun List<GeneratedStudyCard>.toStoredStudyCardDtos(): List<StoredStudyCardDto> {
        return map { card ->
            StoredStudyCardDto(
                cardId = card.cardId,
                cardType = card.cardType.name,
                prompt = card.prompt,
                expectedAnswer = card.expectedAnswer,
                evaluationMode = card.evaluationMode.name,
                isActive = card.isActive,
                acceptedAnswers = card.acceptedAnswers,
                hint = card.hint,
                explanation = card.explanation,
                sourceField = card.sourceField,
            )
        }
    }

    private fun List<GeneratedNoteQualityCheck>.toStoredQualityCheckDtos(): List<StoredNoteQualityCheckDto> {
        return map { check ->
            StoredNoteQualityCheckDto(
                code = check.code.name,
                passed = check.passed,
                message = check.message,
            )
        }
    }

    private fun StoredStudyCardDto.toDomain(): GeneratedStudyCard {
        return GeneratedStudyCard(
            cardId = cardId,
            cardType = enumValueOrDefault(cardType, StudyCardType.Recognition),
            prompt = prompt,
            expectedAnswer = expectedAnswer,
            evaluationMode = enumValueOrDefault(evaluationMode, EvaluationMode.ManualSelfCheck),
            isActive = isActive,
            acceptedAnswers = acceptedAnswers,
            hint = hint,
            explanation = explanation,
            sourceField = sourceField,
        )
    }

    private fun StoredNoteQualityCheckDto.toDomain(): GeneratedNoteQualityCheck {
        return GeneratedNoteQualityCheck(
            code = enumValueOrDefault(code, GeneratedNoteQualityCode.RequiredFieldsPresent),
            passed = passed,
            message = message,
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T {
        return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
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

private data class EncodedFlashcardArtifacts(
    val irregularFormsJson: String,
    val collocationsJson: String,
    val confusableWithJson: String,
    val warningsJson: String,
    val studyCardsJson: String,
    val qualityChecksJson: String,
)
