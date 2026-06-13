package com.emm.data.export

import kotlinx.serialization.Serializable

/**
 * Root JSON envelope for backup export/import.
 * Maps 1:1 to the spec JSON schema.
 */
@Serializable
data class BackupEnvelope(
    val schemaVersion: Int,
    val exportedAt: Long,
    val decks: List<DeckDto>,
    val flashcards: List<FlashcardDto>,
    val examples: List<FlashcardExampleDto>,
    val tags: List<TagDto>,
    val deckTags: List<DeckTagDto>,
    val reviewEvents: List<ReviewEventDto>,
    val reviewProjections: List<ReviewProjectionDto>,
)

@Serializable
data class DeckDto(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Serializable
data class FlashcardDto(
    val id: String,
    val deckId: String,
    val word: String,
    val meaning: String,
    val translation: String?,
    val phonetic: String?,
    val partOfSpeech: String?,
    val type: String?,
    val note: String?,
    val register: String?,
    val levelBand: String?,
    val domain: String?,
    val lemma: String?,
    val whyUseful: String?,
    val usagePattern: String?,
    val irregularFormsJson: String?,
    val collocationsJson: String?,
    val commonMistake: String?,
    val confusableWithJson: String?,
    val clozeSentence: String?,
    val sourceContext: String?,
    val warningsJson: String?,
    val studyCardsJson: String?,
    val qualityChecksJson: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Serializable
data class FlashcardExampleDto(
    val id: String,
    val flashcardId: String,
    val text: String,
    val translation: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Serializable
data class TagDto(
    val id: String,
    val name: String,
    val createdAt: Long,
    val deletedAt: Long?,
)

@Serializable
data class DeckTagDto(
    val tagId: String,
    val deckId: String,
    val createdAt: Long,
)

@Serializable
data class ReviewEventDto(
    val eventId: String,
    val flashcardId: String,
    val grade: String,
    val reviewedAt: Long,
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Long,
    val repetitions: Long,
    val lapses: Long,
    val createdAt: Long,
    // FSRS-6 (schema v2): 1=AGAIN, 2=HARD, 3=GOOD, 4=EASY; 0=legacy. Default allows v1 backup imports.
    val rating: Long = 0L,
)

@Serializable
data class ReviewProjectionDto(
    val flashcardId: String,
    val lastReviewedAt: Long,
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Long,
    val repetitions: Long,
    val lapses: Long,
    val sourceEventId: String,
    val updatedAt: Long,
    // FSRS-6 (schema v2): card scheduling state. Default allows v1 backup imports.
    val state: String = "NEW",
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
)
