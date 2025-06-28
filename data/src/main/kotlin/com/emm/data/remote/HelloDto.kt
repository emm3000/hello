package com.emm.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class HelloDto(
    val androidId: String,
    val decks: List<DeckDto>,
    val flashcards: List<CardDto>,
    val flashcardExamples: List<ExampleDto>,
    val quotes: List<QuoteDto>,
)

@Serializable
data class DeckDto(
    val deckId: String,
    val name: String,
    val description: String,
    val createdAt: Long,
)

@Serializable
data class CardDto(
    val id: String,
    val word: String,
    val meaning: String,
    val translation: String,
    val phonetic: String,
    val audioPath: String,
    val imagePath: String,
    val note: String,
    val createdAt: Long,
    val isGenerated: Int,
    val deckId: String,
)

@Serializable
data class ExampleDto(
    val id: String,
    val text: String,
    val translation: String,
    val type: String,
    val flashcardId: String,
)

@Serializable
data class QuoteDto(
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
    val createdAt: Long,
)
