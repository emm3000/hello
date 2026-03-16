package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.Flashcard

sealed interface StudyUiIntent {
    data class ReviewAnswered(
        val flashcard: Flashcard?,
        val reviewGrade: ReviewGrade,
    ) : StudyUiIntent
}
