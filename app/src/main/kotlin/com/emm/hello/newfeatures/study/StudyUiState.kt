package com.emm.hello.newfeatures.study

import com.emm.hello.core.mvi.MviState

data class StudyUiState(
    val isLoading: Boolean = true,
    val loadError: StudyLoadError? = null,
    val currentItem: StudySessionItem? = null,
    val reviewedCount: Int = 0,
    val knewCount: Int = 0,
    val forgotCount: Int = 0,
    val totalCount: Int = 0,
    val sessionFinished: Boolean = false,
) : MviState

sealed interface StudyLoadError {
    data object SessionLoadFailed : StudyLoadError
}
