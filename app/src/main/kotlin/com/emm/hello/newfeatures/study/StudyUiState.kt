package com.emm.hello.newfeatures.study

data class StudyUiState(
    val currentItem: StudySessionItem? = null,
    val reviewedCount: Int = 0,
    val totalCount: Int = 0,
    val sessionFinished: Boolean = false,
)
