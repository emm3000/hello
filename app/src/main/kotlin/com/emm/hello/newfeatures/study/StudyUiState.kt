package com.emm.hello.newfeatures.study

import com.emm.hello.core.mvi.MviState

data class StudyUiState(
    val currentItem: StudySessionItem? = null,
    val reviewedCount: Int = 0,
    val totalCount: Int = 0,
    val sessionFinished: Boolean = false,
) : MviState
