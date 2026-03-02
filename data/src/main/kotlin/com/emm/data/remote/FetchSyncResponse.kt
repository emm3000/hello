package com.emm.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class FetchSyncResponse(
    val decks: List<DeckResponse>,
    val flashcards: List<CardResponse>,
    val flashcardExamples: List<ExampleResponse>,
    val quotes: List<QuoteResponse>,
    val flashcardReviews: List<FlashcardReviewResponse>,
)

@Serializable
data class DeckResponse(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class CardResponse(
    val id: String,
    val word: String,
    val meaning: String,
    val translation: String,
    val phonetic: String,
    val audioPath: String,
    val imagePath: String,
    val note: String,
    val createdAt: String,
    val updatedAt: String?,
    val isGenerated: Int,
    val deckId: String,
)

@Serializable
data class ExampleResponse(
    val id: String,
    val text: String,
    val translation: String,
    val type: String,
    val flashcardId: String,
    val createdAt: String?,
    val updatedAt: String?,
)

@Serializable
data class QuoteResponse(
    val id: String,
    val title: String,
    val phrase: String,
    val description: String,
    val translation: String,
    val example: String,
    val context: String,
    val pronunciation: String,
    val formality: String,
    val tags: String,
    val category: String?,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class FlashcardReviewResponse(
    val flashcardId: String,
    val lastReviewedAt: Long,
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Long,
    val repetitions: Long,
    val lapses: Long,
    val createdAt: String,
    val updatedAt: String,
)
