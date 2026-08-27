package com.emm.domain.library

import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId

data class LibraryFlashcard(
    val id: FlashcardId,
    val deckId: DeckId,
    val deckName: String,
    val word: String,
    val translation: String,
    val meaning: String,
    val enrichmentStatus: EnrichmentStatus,
    val nextReviewAt: Long?,
)
