package com.emm.data.flashcard

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock

typealias FlashcardEntity = com.emm.data.Flashcard
typealias ReviewProjectionEntity = com.emm.data.ReviewProjection

fun FlashcardEntity.toDomain(): Flashcard {
    val flashcardId = id.toFlashcardId()
    return Flashcard(
        id = flashcardId,
        word = word,
        meaning = meaning,
        translation = translation.orEmpty(),
        examples = emptyList(),
        phonetic = phonetic.orEmpty(),
        review = FsrsCard.new(flashcardId, SystemClock),
        partOfSpeech = partOfSpeech.orEmpty(),
        noteType = type.orEmpty(),
        noteSummary = note.orEmpty(),
    )
}

@JvmName("toDomainFlashcardEntity")
fun List<FlashcardEntity>.toDomain() = map(FlashcardEntity::toDomain)

fun ReviewProjectionEntity.toDomainFromProjection() = FsrsCard(
    flashcardId = flashcardId.toFlashcardId(),
    state = FsrsState.valueOf(state),
    stability = stability,
    difficulty = difficulty,
    lastReviewedAt = lastReviewedAt,
    nextReviewAt = nextReviewAt,
    interval = interval,
    reps = repetitions,
    lapses = lapses,
)

fun List<ReviewProjectionEntity>.toDomainFromProjection() = map(ReviewProjectionEntity::toDomainFromProjection)
