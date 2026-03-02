package com.emm.data.flashcard.iadto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlashcardDto(

    @SerialName("audio_url")
    val audioUrl: String?,

    @SerialName("image_prompt")
    val imagePrompt: String,

    val language: String,

    val meaning: String,

    val phonetic: String,

    val translation: String,

    val word: String,

    val examples: List<ExampleDto>,
)
