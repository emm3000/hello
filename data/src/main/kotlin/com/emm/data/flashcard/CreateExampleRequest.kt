package com.emm.data.flashcard

import kotlinx.serialization.Serializable

@Serializable
data class CreateExampleRequest(
    val id: String,
    val flashcardId: String?,
    val text: String,
    val translation: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
)
