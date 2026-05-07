package com.emm.domain.flashcard

import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedStudyCard

data class FlashcardDetail(
    val flashcard: Flashcard,
    val studyCards: List<GeneratedStudyCard> = emptyList(),
    val qualityChecks: List<GeneratedNoteQualityCheck> = emptyList(),
)
