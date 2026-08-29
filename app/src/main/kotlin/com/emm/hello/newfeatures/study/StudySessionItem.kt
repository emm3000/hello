package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.ids.FlashcardId
import com.emm.domain.study.StudyFlashcard

enum class StudyDirection { RECOGNITION, PRODUCTION }

data class StudySessionItem(
    val flashcardId: FlashcardId,
    val review: FsrsCard,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val translation: String,
    val direction: StudyDirection,
    val usagePattern: String = "",
    val irregularForms: List<String> = emptyList(),
    val partOfSpeech: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
) {
    val cue: String
        get() = translation.ifBlank { meaning }
}

internal fun StudySessionItem.revealsWordOn(face: CardFace): Boolean =
    direction == StudyDirection.RECOGNITION || face == CardFace.Back

internal fun StudyFlashcard.toStudySessionItem(): StudySessionItem = StudySessionItem(
    flashcardId = flashcardId,
    review = review,
    word = word,
    phonetic = phonetic,
    meaning = meaning,
    translation = translation,
    direction = if (review.productionSince != null) StudyDirection.PRODUCTION else StudyDirection.RECOGNITION,
    usagePattern = usagePattern,
    irregularForms = irregularForms,
    partOfSpeech = partOfSpeech,
    example = example,
    exampleTranslation = exampleTranslation,
)
