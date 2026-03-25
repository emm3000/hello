package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.GeneratedStudyCard

data class StudySessionItem(
    val flashcard: Flashcard,
    val studyCard: GeneratedStudyCard,
)

internal fun Flashcard.toStudySessionItems(): List<StudySessionItem> {
    return studyCards
        .filter { it.isActive }
        .map { StudySessionItem(flashcard = this, studyCard = it) }
}
