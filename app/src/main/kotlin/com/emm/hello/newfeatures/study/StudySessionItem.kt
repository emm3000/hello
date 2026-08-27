package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.ids.FlashcardId
import com.emm.domain.study.StudyFlashcard

/**
 * One flashcard on screen during a study session.
 *
 * A session shows every due flashcard exactly once and persists exactly one review per
 * flashcard, so this is a 1:1 projection of [StudyFlashcard] onto the fields the card faces
 * render. Generated study cards are deliberately not part of it.
 */
data class StudySessionItem(
    val flashcardId: FlashcardId,
    val review: FsrsCard,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val translation: String,
    val usagePattern: String = "",
    val irregularForms: List<String> = emptyList(),
)

internal fun StudyFlashcard.toStudySessionItem(): StudySessionItem = StudySessionItem(
    flashcardId = flashcardId,
    review = review,
    word = word,
    phonetic = phonetic,
    meaning = meaning,
    translation = translation,
    usagePattern = usagePattern,
    irregularForms = irregularForms,
)
