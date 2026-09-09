package com.emm.hello.newfeatures.capture

import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.FlashcardId
import com.emm.hello.core.mvi.MviState

data class CaptureUiState(
    val word: String = "",
    val targetDeck: Deck? = null,
    val decks: List<Deck> = emptyList(),
    val isDeckPickerOpen: Boolean = false,
    val isSaving: Boolean = false,
    val pending: Int = 0,
    val failed: Int = 0,
    val recentCaptures: List<RecentCapture> = emptyList(),
    val isOnline: Boolean = true,
    val isManual: Boolean = false,
    val translation: String = "",
    val meaning: String = "",
) : MviState {

    val canSubmit: Boolean
        get() = word.isNotBlank() &&
            targetDeck != null &&
            !isSaving &&
            (!isManual || translation.isNotBlank())

    val hasBacklog: Boolean
        get() = pending > 0 || failed > 0
}

data class RecentCapture(
    val flashcardId: FlashcardId,
    val word: String,
    val status: EnrichmentStatus,
    val failureReason: String? = null,
)
