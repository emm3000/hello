package com.emm.hello.newfeatures.deck

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface DeckDetailUiEffect : MviEffect {
    data class NavigateToEditDeck(val deckId: String) : DeckDetailUiEffect
    data object DeckDeleted : DeckDetailUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : DeckDetailUiEffect
    data class ShowUndoCardDeleted(
        val flashcardId: String,
        val deletedAt: Long,
    ) : DeckDetailUiEffect
}
