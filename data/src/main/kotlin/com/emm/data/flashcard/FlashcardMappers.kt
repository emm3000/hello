package com.emm.data.flashcard

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview

typealias FlashcardEntity = com.emm.data.Flashcard
typealias ReviewProjectionEntity = com.emm.data.ReviewProjection

fun FlashcardEntity.toDomain() = Flashcard(
    id = id,
    word = word,
    meaning = meaning,
    translation = translation.orEmpty(),
    examples = emptyList(),
    phonetic = phonetic.orEmpty(),
    review = FlashcardReview.Empty,
    partOfSpeech = partOfSpeech.orEmpty(),
    type = type.orEmpty(),
    note = note.orEmpty(),
)

@JvmName("toDomainFlashcardEntity")
fun List<FlashcardEntity>.toDomain() = map(FlashcardEntity::toDomain)

fun ReviewProjectionEntity.toDomainFromProjection() = FlashcardReview(
    flashcardId = flashcardId,
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
    easeFactor = easeFactor,
    interval = interval,
    repetitions = repetitions,
    lapses = lapses,
)

fun List<ReviewProjectionEntity>.toDomainFromProjection() = map(ReviewProjectionEntity::toDomainFromProjection)
