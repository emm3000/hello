package com.emm.data.flashcard.iadto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlashcardDto(

    val word: String,

    val meaning: String,

    val translation: String,

    val phonetic: String,

    val language: String,

    @SerialName("part_of_speech")
    val partOfSpeech: String = "",

    val type: String = "",

    val notes: String = "",

    val tags: List<String> = emptyList(),

    val examples: List<ExampleDto> = emptyList(),

    val conjugation: ConjugationDto? = null,
)

@Serializable
data class ConjugationDto(
    val present: String = "",
    val past: String = "",
    val participle: String = "",
    val gerund: String = "",
)
