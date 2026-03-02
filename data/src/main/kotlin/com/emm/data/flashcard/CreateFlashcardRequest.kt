package com.emm.data.flashcard

import kotlinx.serialization.Serializable

@Serializable
data class CreateFlashcardRequest(
    val id: String,
    val deckId: String,
    val word: String,
    val meaning: String,
    val translation: String,
    val phonetic: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
