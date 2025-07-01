package com.emm.data.remote

import com.emm.data.Deck
import com.emm.data.Flashcard
import com.emm.data.FlashcardExample
import com.emm.data.FlashcardReview
import com.emm.data.Quote

data class HolderOfDatabaseTables(
    val decks: List<Deck>,
    val flashcards: List<Flashcard>,
    val examples: List<FlashcardExample>,
    val reviews: List<FlashcardReview>,
    val quotes: List<Quote>,
)