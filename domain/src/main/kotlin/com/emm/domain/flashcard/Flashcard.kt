package com.emm.domain.flashcard

data class Flashcard(
    val id: String,
    val word: String,
    val meaning: String,
    val translation: String,
    val examples: List<Example>,
    val phonetic: String,
    val review: FlashcardReview,
) {

    companion object {

        val Empty: Flashcard
            get() = Flashcard(
                id = "",
                word = "",
                meaning = "",
                translation = "",
                examples = emptyList(),
                phonetic = "",
                review = FlashcardReview.Empty,
            )
    }
}