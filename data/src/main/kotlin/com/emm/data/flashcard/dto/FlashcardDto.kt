package com.emm.data.flashcard.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlashcardDto(

    @SerialName("audio_url")
    val audioUrl: String?,

    @SerialName("example")
    val example: String,

    @SerialName("image_prompt")

    val imagePrompt: String,
    @SerialName("language")

    val language: String,
    @SerialName("meaning")

    val meaning: String,
    @SerialName("phonetic")

    val phonetic: String,

    @SerialName("translation")
    val translation: String,

    @SerialName("word")
    val word: String,
)