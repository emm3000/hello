package com.emm.hello.newfeatures.study

import com.emm.domain.study.ReviewGrade
import com.emm.hello.core.mvi.MviIntent

sealed interface StudyUiIntent : MviIntent {
    data object FinishDialogDismissed : StudyUiIntent
    data object CreateCardClicked : StudyUiIntent
    data object RetryLoad : StudyUiIntent
    data object ExitClicked : StudyUiIntent

    data class ReviewAnswered(
        val item: StudySessionItem?,
        val reviewGrade: ReviewGrade,
    ) : StudyUiIntent
}
