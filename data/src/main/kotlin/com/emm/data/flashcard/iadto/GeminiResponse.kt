package com.emm.data.flashcard.iadto

import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponse(
    val success: Boolean,
    val data: FlashcardDto? = null,
    val error: ResponseError? = null,
)
