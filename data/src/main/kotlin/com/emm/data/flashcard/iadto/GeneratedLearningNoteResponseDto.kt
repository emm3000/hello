package com.emm.data.flashcard.iadto

import kotlinx.serialization.Serializable

@Serializable
data class GeneratedLearningNoteResponseDto(
    val success: Boolean,
    val data: GeneratedLearningNoteDto? = null,
    val error: ResponseError? = null,
)
