package com.emm.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val androidId: String,
    val decks: List<DeckUpsertRequest>,
    val flashcards: List<CardUpsertRequest>,
    val flashcardExamples: List<ExampleUpsertRequest>,
    val quotes: List<QuoteUpsertRequest>,
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
    val audioPath: String,
    val imagePath: String,
    val note: String,
    val createdAt: String,
    val updatedAt: String,
    val isGenerated: Int,
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
