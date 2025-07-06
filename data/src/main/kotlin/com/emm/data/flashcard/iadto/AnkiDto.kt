package com.emm.data.flashcard.iadto

import kotlinx.serialization.Serializable

@Serializable
data class AnkiDto(
    val category: String,
    val complexity: String,
    val front: String,
    val back: String,
)