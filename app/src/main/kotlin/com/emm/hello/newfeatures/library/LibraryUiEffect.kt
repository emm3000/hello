package com.emm.hello.newfeatures.library

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface LibraryUiEffect : MviEffect {
    data class OpenCard(val cardId: String, val deckId: String) : LibraryUiEffect
    data object OpenCapture : LibraryUiEffect
    data class ShowUndoCardDeleted(val flashcardId: String, val deletedAt: Long) : LibraryUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : LibraryUiEffect
}
