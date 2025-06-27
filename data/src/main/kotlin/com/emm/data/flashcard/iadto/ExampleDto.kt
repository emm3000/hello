package com.emm.data.flashcard.iadto

import kotlinx.serialization.Serializable

@Serializable
data class ExampleDto(
    val text: String,
    val translation: String,
    val type: String,
)