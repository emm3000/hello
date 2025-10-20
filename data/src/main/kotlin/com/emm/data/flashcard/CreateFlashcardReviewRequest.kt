package com.emm.data.flashcard

import kotlinx.serialization.Serializable

@Serializable
class CreateFlashcardReviewRequest(
    val flashcardId: String,
    val lastReviewedAt: Long,
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Long,
    val repetitions: Long,
    val lapses: Long,
    val createdAt: Long,
    val updatedAt: Long,
)