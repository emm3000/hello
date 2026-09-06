package com.emm.hello.newfeatures.capture

import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.ids.FlashcardId
import com.emm.hello.core.mvi.MviState

data class CaptureUiState(
    val word: String = "",
    val targetDeck: Deck? = null,
    val isSaving: Boolean = false,
    val pending: Int = 0,
    val failed: Int = 0,
    val recentCaptures: List<RecentCapture> = emptyList(),
    val isOnline: Boolean = true,
) : MviState {

    val canSubmit: Boolean
        get() = word.isNotBlank() && targetDeck != null && !isSaving

    val hasBacklog: Boolean
        get() = pending > 0 || failed > 0
}

data class RecentCapture(
    val flashcardId: FlashcardId,
    val word: String,
    val status: EnrichmentStatus,
)
