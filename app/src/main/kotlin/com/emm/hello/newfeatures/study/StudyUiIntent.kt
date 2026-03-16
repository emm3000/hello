package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.study.ReviewGrade

sealed interface StudyUiIntent {
    data object BackClicked : StudyUiIntent
    data object FinishDialogDismissed : StudyUiIntent

    data class ReviewAnswered(
        val flashcard: Flashcard?,
        val reviewGrade: ReviewGrade,
    ) : StudyUiIntent
}
