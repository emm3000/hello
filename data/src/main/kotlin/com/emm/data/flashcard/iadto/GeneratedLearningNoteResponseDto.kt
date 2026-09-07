package com.emm.data.flashcard.iadto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeneratedLearningNoteResponseDto(
    val success: Boolean,
    val data: GeneratedLearningNoteDto? = null,
    val error: ResponseError? = null,
    val meta: GeneratedLearningNoteMetaDto? = null,
)

@Serializable
data class GeneratedLearningNoteMetaDto(
    @SerialName("prompt_version") val promptVersion: Int = 0,
)
