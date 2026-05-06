package com.emm.domain.generation

import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.StudyCardType

data class GeneratedStudyCard(
    val cardId: String,
    val cardType: StudyCardType,
    val prompt: String,
    val expectedAnswer: String,
    val evaluationMode: EvaluationMode,
    val isActive: Boolean = true,
    val acceptedAnswers: List<String> = emptyList(),
    val hint: String = "",
    val explanation: String = "",
    val sourceField: String = "",
)
