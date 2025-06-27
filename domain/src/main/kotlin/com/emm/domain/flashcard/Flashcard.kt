package com.emm.domain.flashcard

data class Flashcard(
    val id: String,
    val word: String,
    val meaning: String,
    val translation: String,
    val example: String,
    val phonetic: String,
)
