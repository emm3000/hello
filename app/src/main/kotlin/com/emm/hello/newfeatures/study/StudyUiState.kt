package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.Flashcard

data class StudyUiState(
    val currentFlashcard: Flashcard? = null,
    val reviewedCount: Int = 0,
    val totalCount: Int = 0,
    val sessionFinished: Boolean = false,
)
