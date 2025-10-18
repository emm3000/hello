package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.Flashcard

data class StudyUiState(
    val currentFlashcard: Flashcard? = null,
    val isFinished: Boolean = false,
)