package com.emm.data.flashcard

import com.emm.data.FlashcardWithExamples
import com.emm.data.FlashcardsToReviewAllDecks
import com.emm.data.FlashcardsToReviewByDeck
import com.emm.data.flashcard.iadto.StoredNoteQualityCheckDto
import com.emm.data.flashcard.iadto.StoredStudyCardDto
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedNoteQualityCode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.StudyCardType
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.study.StudyFlashcard
import com.emm.domain.time.SystemClock
import kotlinx.serialization.json.Json

internal fun toDomainSummary(
    entity: FlashcardEntity,
    json: Json,
    review: FsrsCard = FsrsCard.new(entity.id.toFlashcardId(), SystemClock),
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
        irregularForms = decodeStringList(entity.irregularFormsJson, json),
        collocations = decodeStringList(entity.collocationsJson, json),
        commonMistake = entity.commonMistake.orEmpty(),
        confusableWith = decodeStringList(entity.confusableWithJson, json),
        clozeSentence = entity.clozeSentence.orEmpty(),
        sourceContext = entity.sourceContext.orEmpty(),
        warnings = decodeStringList(entity.warningsJson, json),
        enrichmentStatus = toEnrichmentStatus(entity.enrichmentStatus),
    )
}

internal fun toDomainSummary(
    entity: FlashcardsToReviewByDeck,
    review: FsrsCard,
    json: Json,
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
        irregularForms = decodeStringList(entity.irregularFormsJson, json),
        collocations = decodeStringList(entity.collocationsJson, json),
        commonMistake = entity.commonMistake.orEmpty(),
        confusableWith = decodeStringList(entity.confusableWithJson, json),
        clozeSentence = entity.clozeSentence.orEmpty(),
        sourceContext = entity.sourceContext.orEmpty(),
        warnings = decodeStringList(entity.warningsJson, json),
        enrichmentStatus = toEnrichmentStatus(entity.enrichmentStatus),
    )
}

internal fun toDomainSummary(
    entity: com.emm.data.FlashcardsWithReview,
    review: FsrsCard,
    json: Json,
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
        irregularForms = decodeStringList(entity.irregularFormsJson, json),
        collocations = decodeStringList(entity.collocationsJson, json),
        commonMistake = entity.commonMistake.orEmpty(),
        confusableWith = decodeStringList(entity.confusableWithJson, json),
        clozeSentence = entity.clozeSentence.orEmpty(),
        sourceContext = entity.sourceContext.orEmpty(),
        warnings = decodeStringList(entity.warningsJson, json),
        enrichmentStatus = toEnrichmentStatus(entity.enrichmentStatus),
    )
}

internal fun toDomainDetail(
    entity: FlashcardWithExamples,
    examples: List<Example>,
    json: Json,
    review: FsrsCard,
): Flashcard {
    return Flashcard(
        id = entity.id.toFlashcardId(),
        word = entity.word,
        meaning = entity.meaning,
        translation = entity.translation.orEmpty(),
        phonetic = entity.phonetic.orEmpty(),
        examples = examples,
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
        irregularForms = decodeStringList(entity.irregularFormsJson, json),
        collocations = decodeStringList(entity.collocationsJson, json),
        commonMistake = entity.commonMistake.orEmpty(),
        confusableWith = decodeStringList(entity.confusableWithJson, json),
        clozeSentence = entity.clozeSentence.orEmpty(),
        sourceContext = entity.sourceContext.orEmpty(),
        warnings = decodeStringList(entity.warningsJson, json),
        enrichmentStatus = toEnrichmentStatus(entity.enrichmentStatus),
    )
}

internal fun toStudyFlashcard(
    entity: FlashcardsToReviewByDeck,
    review: FsrsCard,
    json: Json,
): StudyFlashcard {
    return StudyFlashcard(
        flashcardId = entity.id.toFlashcardId(),
        word = entity.word,
        phonetic = entity.phonetic.orEmpty(),
        meaning = entity.meaning,
        translation = entity.translation.orEmpty(),
        review = review,
        studyCards = decodeStudyCards(entity.studyCardsJson, json),
        usagePattern = entity.usagePattern.orEmpty(),
        whyUseful = entity.whyUseful.orEmpty(),
        sourceContext = entity.sourceContext.orEmpty(),
        irregularForms = decodeStringList(entity.irregularFormsJson, json),
        partOfSpeech = entity.partOfSpeech.orEmpty(),
        example = entity.exampleText.orEmpty(),
        exampleTranslation = entity.exampleTranslation.orEmpty(),
        enrichmentStatus = toEnrichmentStatus(entity.enrichmentStatus),
    )
}

internal fun toStudyFlashcard(
    entity: FlashcardsToReviewAllDecks,
    review: FsrsCard,
    json: Json,
): StudyFlashcard {
    return StudyFlashcard(
        flashcardId = entity.id.toFlashcardId(),
        word = entity.word,
        phonetic = entity.phonetic.orEmpty(),
        meaning = entity.meaning,
        translation = entity.translation.orEmpty(),
        review = review,
        studyCards = decodeStudyCards(entity.studyCardsJson, json),
        usagePattern = entity.usagePattern.orEmpty(),
        whyUseful = entity.whyUseful.orEmpty(),
        sourceContext = entity.sourceContext.orEmpty(),
        irregularForms = decodeStringList(entity.irregularFormsJson, json),
        partOfSpeech = entity.partOfSpeech.orEmpty(),
        example = entity.exampleText.orEmpty(),
        exampleTranslation = entity.exampleTranslation.orEmpty(),
        enrichmentStatus = toEnrichmentStatus(entity.enrichmentStatus),
    )
}

internal fun toStudyFlashcard(
    entity: com.emm.data.FlashcardsWithReview,
    review: FsrsCard,
    json: Json,
): StudyFlashcard {
    return StudyFlashcard(
        flashcardId = entity.id.toFlashcardId(),
        word = entity.word,
        phonetic = entity.phonetic.orEmpty(),
        meaning = entity.meaning,
        translation = entity.translation.orEmpty(),
        review = review,
        studyCards = decodeStudyCards(entity.studyCardsJson, json),
        usagePattern = entity.usagePattern.orEmpty(),
        whyUseful = entity.whyUseful.orEmpty(),
        sourceContext = entity.sourceContext.orEmpty(),
        irregularForms = decodeStringList(entity.irregularFormsJson, json),
        partOfSpeech = entity.partOfSpeech.orEmpty(),
        example = entity.exampleText.orEmpty(),
        exampleTranslation = entity.exampleTranslation.orEmpty(),
        enrichmentStatus = toEnrichmentStatus(entity.enrichmentStatus),
    )
}

internal fun toExampleOrNull(item: FlashcardWithExamples): Example? {
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

/**
 * Maps the joined ReviewProjection columns of a due-card query row to an [FsrsCard].
 *
 * The LEFT JOIN means every ReviewProjection column is nullable in the generated row type;
 * a never-reviewed card (no ReviewProjection row at all) has every field null and maps to a
 * fresh [FsrsCard.new]. `nextReviewAt` is intentionally NOT stamped with "now" here: a
 * never-reviewed card or a card whose stored nextReviewAt is already in the past must keep
 * surfacing as due — clobbering it with the live clock would break the
 * `nextReviewAt >= lastReviewedAt` invariant once real review data is read elsewhere.
 */
internal fun mapFsrsCard(deck: FlashcardsToReviewByDeck): FsrsCard {
    val hasMissingField = listOf(
        deck.flashcardId,
        deck.lastReviewedAt,
        deck.nextReviewAt,
        deck.interval,
        deck.repetitions,
        deck.lapses,
        deck.state,
        deck.stability,
        deck.difficulty,
    ).any { it == null }

    if (hasMissingField) return FsrsCard.new((deck.flashcardId ?: "empty-flashcard").toFlashcardId(), SystemClock)

    return FsrsCard(
        flashcardId = deck.flashcardId.orEmpty().toFlashcardId(),
        state = enumValueOrDefault(deck.state.orEmpty(), FsrsState.NEW),
        stability = deck.stability ?: 0.0,
        difficulty = deck.difficulty ?: 0.0,
        lastReviewedAt = deck.lastReviewedAt ?: 0L,
        nextReviewAt = deck.nextReviewAt ?: 0L,
        interval = deck.interval ?: 0L,
        reps = deck.repetitions ?: 0L,
        lapses = deck.lapses ?: 0L,
    )
}

internal fun mapFsrsCard(deck: FlashcardsToReviewAllDecks): FsrsCard {
    val hasMissingField = listOf(
        deck.flashcardId,
        deck.lastReviewedAt,
        deck.nextReviewAt,
        deck.interval,
        deck.repetitions,
        deck.lapses,
        deck.state,
        deck.stability,
        deck.difficulty,
    ).any { it == null }

    if (hasMissingField) return FsrsCard.new((deck.flashcardId ?: "empty-flashcard").toFlashcardId(), SystemClock)

    return FsrsCard(
        flashcardId = deck.flashcardId.orEmpty().toFlashcardId(),
        state = enumValueOrDefault(deck.state.orEmpty(), FsrsState.NEW),
        stability = deck.stability ?: 0.0,
        difficulty = deck.difficulty ?: 0.0,
        lastReviewedAt = deck.lastReviewedAt ?: 0L,
        nextReviewAt = deck.nextReviewAt ?: 0L,
        interval = deck.interval ?: 0L,
        reps = deck.repetitions ?: 0L,
        lapses = deck.lapses ?: 0L,
    )
}

/**
 * Maps the joined ReviewProjection columns of the deck-detail "with review" query row to an
 * [FsrsCard]. Mirrors the [FlashcardsToReviewByDeck]/[FlashcardsToReviewAllDecks] overloads above:
 * a never-reviewed card (no ReviewProjection row) maps to a fresh [FsrsCard.new].
 */
internal fun mapFsrsCard(deck: com.emm.data.FlashcardsWithReview): FsrsCard {
    val hasMissingField = listOf(
        deck.flashcardId,
        deck.lastReviewedAt,
        deck.nextReviewAt,
        deck.interval,
        deck.repetitions,
        deck.lapses,
        deck.state,
        deck.stability,
        deck.difficulty,
    ).any { it == null }

    if (hasMissingField) return FsrsCard.new((deck.flashcardId ?: "empty-flashcard").toFlashcardId(), SystemClock)

    return FsrsCard(
        flashcardId = deck.flashcardId.orEmpty().toFlashcardId(),
        state = enumValueOrDefault(deck.state.orEmpty(), FsrsState.NEW),
        stability = deck.stability ?: 0.0,
        difficulty = deck.difficulty ?: 0.0,
        lastReviewedAt = deck.lastReviewedAt ?: 0L,
        nextReviewAt = deck.nextReviewAt ?: 0L,
        interval = deck.interval ?: 0L,
        reps = deck.repetitions ?: 0L,
        lapses = deck.lapses ?: 0L,
    )
}

/**
 * Maps the joined ReviewProjection columns of the Card Detail "with examples" query row to an
 * [FsrsCard]. Mirrors the [FlashcardsToReviewByDeck]/[FlashcardsToReviewAllDecks] overloads above:
 * a never-reviewed card (no ReviewProjection row) maps to a fresh [FsrsCard.new].
 */
internal fun mapFsrsCard(deck: FlashcardWithExamples): FsrsCard {
    val hasMissingField = listOf(
        deck.flashcardId,
        deck.lastReviewedAt,
        deck.nextReviewAt,
        deck.interval,
        deck.repetitions,
        deck.lapses,
        deck.state,
        deck.stability,
        deck.difficulty,
    ).any { it == null }

    if (hasMissingField) return FsrsCard.new((deck.flashcardId ?: "empty-flashcard").toFlashcardId(), SystemClock)

    return FsrsCard(
        flashcardId = deck.flashcardId.orEmpty().toFlashcardId(),
        state = enumValueOrDefault(deck.state.orEmpty(), FsrsState.NEW),
        stability = deck.stability ?: 0.0,
        difficulty = deck.difficulty ?: 0.0,
        lastReviewedAt = deck.lastReviewedAt ?: 0L,
        nextReviewAt = deck.nextReviewAt ?: 0L,
        interval = deck.interval ?: 0L,
        reps = deck.repetitions ?: 0L,
        lapses = deck.lapses ?: 0L,
    )
}

internal fun decodeStringList(raw: String?, json: Json): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}

internal fun decodeStudyCards(raw: String?, json: Json): List<GeneratedStudyCard> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        json.decodeFromString<List<StoredStudyCardDto>>(raw).map(StoredStudyCardDto::toDomain)
    }.getOrDefault(emptyList())
}

internal fun decodeQualityChecks(raw: String?, json: Json): List<GeneratedNoteQualityCheck> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        json.decodeFromString<List<StoredNoteQualityCheckDto>>(raw).map(StoredNoteQualityCheckDto::toDomain)
    }.getOrDefault(emptyList())
}

internal fun StoredStudyCardDto.toDomain(): GeneratedStudyCard {
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

internal fun StoredNoteQualityCheckDto.toDomain(): GeneratedNoteQualityCheck {
    return GeneratedNoteQualityCheck(
        code = enumValueOrDefault(code, GeneratedNoteQualityCode.RequiredFieldsPresent),
        passed = passed,
        message = message,
    )
}

internal inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T {
    return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
}

internal fun toEnrichmentStatus(rawValue: String): EnrichmentStatus {
    return EnrichmentStatus.entries.firstOrNull { it.name == rawValue } ?: EnrichmentStatus.ENRICHED
}
