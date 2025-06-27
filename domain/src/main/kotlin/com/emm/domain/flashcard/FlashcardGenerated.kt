package com.emm.domain.flashcard

data class FlashcardGenerated(
    val word: String,
    val translation: String,
    val phonetics: String,
    val meaning: String,
    val language: String,
    val examples: List<Example>,
)
