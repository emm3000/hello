package com.emm.data.flashcard.iadto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiSuccessDto(

    @SerialName("data")
    val flashcardDto: FlashcardDto,

    @SerialName("success")
    val success: Boolean,
)