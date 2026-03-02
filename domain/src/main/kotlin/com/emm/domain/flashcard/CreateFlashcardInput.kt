package com.emm.domain.flashcard

data class CreateFlashcardInput(
    val id: String? = null,
    val deckId: String,
    val word: String,
    val meaning: String,
    val translation: String,
    val phonetic: String,
)
