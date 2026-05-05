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
import com.emm.data.localfirst.LocalFirstWrite
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
import com.emm.domain.flashcard.RegenerableNoteField
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.flashcard.StudySessionRepository
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@LocalFirstWrite
class DefaultFlashcardRepository(
    private val db: HelloDb,
    private val geminiService: GeminiService,
    private val json: Json,
) : FlashcardReadRepository,
    FlashcardWriteRepository,
    StudySessionRepository,
    FlashcardGenerationRepository {

    private val dao: FlashcardQueries = db.flashcardQueries

    private val exampleDao: FlashcardExampleQueries = db.flashcardExampleQueries

    override suspend fun create(input: CreateFlashcardInput) = withContext(Dispatchers.IO) {
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
            studyCardsJson = json.encodeToString(input.studyCards.toStoredStudyCardDtos()),
            qualityChecksJson = json.encodeToString(input.qualityChecks.toStoredQualityCheckDtos()),
        )
    }

    override suspend fun upsertExamples(
        examples: List<Example>,
        flashcardId: FlashcardId,
    ) = withContext(Dispatchers.IO) {
        db.transaction { populate(examples, flashcardId.value) }
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

    override suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote =
        withContext(Dispatchers.IO) {
            val prompt = Prompt.buildLearningNotePrompt(input)
            val response = geminiService.process(prompt)
            GeneratedLearningNoteResponseParser.parse(response, json)
        }

    override suspend fun regenerateNoteField(
        input: FlashcardGenerationInput,
        note: GeneratedLearningNote,
        field: RegenerableNoteField,
    ): String = withContext(Dispatchers.IO) {
        val prompt = Prompt.buildNoteFieldRegenerationPrompt(input, note, field)
        val response = geminiService.process(prompt)
        val jsonKey = when (field) {
            RegenerableNoteField.WhyUseful -> "why_useful"
            RegenerableNoteField.UsagePattern -> "usage_pattern"
            RegenerableNoteField.CommonMistake -> "common_mistake"
        }
        PartialRegenerationParser.parseField(response, json, jsonKey = jsonKey, label = field.name)
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
        return dao
            .all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map(::toDomainSummary) }
    }

    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> {
        return dao
            .selectByDeck(deckId.value)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities -> entities.map(::toDomainSummary) }
    }

    override suspend fun fetchById(id: FlashcardId): Flashcard = withContext(Dispatchers.IO) {
        val flashcardEntities: List<FlashcardWithExamples> = dao
            .flashcardWithExamples(id.value)
            .executeAsList()

        val first: FlashcardWithExamples = flashcardEntities.firstOrNull()
            ?: throw NoSuchElementException("Flashcard not found")

        val examples: List<Example> = flashcardEntities.mapNotNull(::toExampleOrNull)
        toDomainDetail(first, examples)
    }

    override suspend fun sessionToday(deckId: DeckId): List<Flashcard> = withContext(Dispatchers.IO) {
        val flashcardsToReviewByDeck: List<FlashcardsToReviewByDeck> = dao.flashcardsToReviewByDeck(
            deckId = deckId.value,
            now = Instant.now().toEpochMilli(),
        ).executeAsList()

        flashcardsToReviewByDeck.map {
            val review: FlashcardReview = mapFlashcardReview(it)
            toDomainSummary(it, review)
        }
    }

    override fun flashcardWithReview(deckId: DeckId): Flow<List<Flashcard>> {
        return dao.flashcardsWithReview(deckId.value).asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map {
                    toDomainSummary(
                        entity = it,
                        review = FlashcardReview.empty(SystemClock).copy(
                            nextReviewAt = it.nextReviewAt ?: Instant.now().toEpochMilli()
                        ),
                    )
                }
            }
    }

    private fun toDomainSummary(
        entity: FlashcardEntity,
        review: FlashcardReview = FlashcardReview.empty(SystemClock),
    ): Flashcard {
        return Flashcard(
            id = entity.id.toFlashcardId(),
            word = entity.word,
            meaning = entity.meaning,
            translation = entity.translation.orEmpty(),
            phonetic = entity.phonetic.orEmpty(),
            examples = emptyList(),
            review = review,
            partOfSpeech = entity.partOfSpeech.orEmpty(),
            noteType = entity.type.orEmpty(),
            noteSummary = entity.note.orEmpty(),
            register = entity.register.orEmpty(),
            levelBand = entity.levelBand.orEmpty(),
            learningDomain = entity.domain.orEmpty(),
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
            id = entity.id.toFlashcardId(),
            word = entity.word,
            meaning = entity.meaning,
            translation = entity.translation.orEmpty(),
            phonetic = entity.phonetic.orEmpty(),
            examples = emptyList(),
            review = review,
            partOfSpeech = entity.partOfSpeech.orEmpty(),
            noteType = entity.type.orEmpty(),
            noteSummary = entity.note.orEmpty(),
            register = entity.register.orEmpty(),
            levelBand = entity.levelBand.orEmpty(),
            learningDomain = entity.domain.orEmpty(),
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
            id = entity.id.toFlashcardId(),
            word = entity.word,
            meaning = entity.meaning,
            translation = entity.translation.orEmpty(),
            phonetic = entity.phonetic.orEmpty(),
            examples = emptyList(),
            review = review,
            partOfSpeech = entity.partOfSpeech.orEmpty(),
            noteType = entity.type.orEmpty(),
            noteSummary = entity.note.orEmpty(),
            register = entity.register.orEmpty(),
            levelBand = entity.levelBand.orEmpty(),
            learningDomain = entity.domain.orEmpty(),
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
            id = entity.id.toFlashcardId(),
            word = entity.word,
            meaning = entity.meaning,
            translation = entity.translation.orEmpty(),
            phonetic = entity.phonetic.orEmpty(),
            examples = examples,
            review = FlashcardReview.empty(SystemClock),
            partOfSpeech = entity.partOfSpeech.orEmpty(),
            noteType = entity.type.orEmpty(),
            noteSummary = entity.note.orEmpty(),
            register = entity.register.orEmpty(),
            levelBand = entity.levelBand.orEmpty(),
            learningDomain = entity.domain.orEmpty(),
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

        if (hasMissingField) return FlashcardReview.empty(SystemClock)

        return FlashcardReview(
            flashcardId = (deck.flashcardId ?: "empty-flashcard").toFlashcardId(),
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
