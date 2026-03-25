package com.emm.data.flashcard.iadto

import kotlinx.serialization.Serializable

@Serializable
data class StoredStudyCardDto(
    val cardId: String,
    val cardType: String,
    val prompt: String,
    val expectedAnswer: String,
    val evaluationMode: String,
    val isActive: Boolean = true,
    val acceptedAnswers: List<String> = emptyList(),
    val hint: String = "",
    val explanation: String = "",
    val sourceField: String = "",
)

@Serializable
data class StoredNoteQualityCheckDto(
    val code: String,
    val passed: Boolean,
    val message: String,
)
