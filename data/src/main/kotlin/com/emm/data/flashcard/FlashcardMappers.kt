package com.emm.data.flashcard

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock

typealias FlashcardEntity = com.emm.data.Flashcard
typealias ReviewProjectionEntity = com.emm.data.ReviewProjection

fun FlashcardEntity.toDomain() = Flashcard(
    id = id.toFlashcardId(),
    word = word,
    meaning = meaning,
    translation = translation.orEmpty(),
    examples = emptyList(),
    phonetic = phonetic.orEmpty(),
    review = FlashcardReview.empty(SystemClock),
    partOfSpeech = partOfSpeech.orEmpty(),
    noteType = type.orEmpty(),
    noteSummary = note.orEmpty(),
)

@JvmName("toDomainFlashcardEntity")
fun List<FlashcardEntity>.toDomain() = map(FlashcardEntity::toDomain)

fun ReviewProjectionEntity.toDomainFromProjection() = FlashcardReview(
    flashcardId = flashcardId.toFlashcardId(),
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
    easeFactor = easeFactor,
    interval = interval,
    repetitions = repetitions,
    lapses = lapses,
)

fun List<ReviewProjectionEntity>.toDomainFromProjection() = map(ReviewProjectionEntity::toDomainFromProjection)
