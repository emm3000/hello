package com.emm.hello.newfeatures.study

import com.emm.domain.study.ReviewGrade

sealed interface StudyUiIntent {
    data object BackClicked : StudyUiIntent
    data object FinishDialogDismissed : StudyUiIntent

    data class ReviewAnswered(
        val item: StudySessionItem?,
        val reviewGrade: ReviewGrade,
    ) : StudyUiIntent
}
