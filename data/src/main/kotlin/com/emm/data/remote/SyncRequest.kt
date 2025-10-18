package com.emm.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val androidId: String,
    val decks: List<DeckUpsertRequest>,
    val flashcards: List<CardUpsertRequest>,
    val flashcardExamples: List<ExampleUpsertRequest>,
    val quotes: List<QuoteUpsertRequest>,
    val flashcardReviews: List<FlashcardReviewUpsertRequest>,
)

@Serializable
data class DeckUpsertRequest(
    val deckId: String,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CardUpsertRequest(
    val id: String,
    val word: String,
    val meaning: String,
    val translation: String,
    val phonetic: String,
    val note: String,
    val createdAt: String,
    val updatedAt: String,
    val deckId: String,
)

@Serializable
data class ExampleUpsertRequest(
    val id: String,
    val text: String,
    val translation: String,
    val type: String,
    val flashcardId: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class QuoteUpsertRequest(
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
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class FlashcardReviewUpsertRequest(
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
