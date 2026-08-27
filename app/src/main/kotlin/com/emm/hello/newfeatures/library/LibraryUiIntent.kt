package com.emm.hello.newfeatures.library

import com.emm.domain.ids.DeckId
import com.emm.domain.library.LibraryFlashcard
import com.emm.hello.core.mvi.MviIntent

sealed interface LibraryUiIntent : MviIntent {
    data class QueryChanged(val value: String) : LibraryUiIntent
    data class DeckFilterToggled(val deckId: DeckId) : LibraryUiIntent
    data object FiltersCleared : LibraryUiIntent
    data class CardOpened(val card: LibraryFlashcard) : LibraryUiIntent
    data object CaptureRequested : LibraryUiIntent
    data class UndoDeleteCard(val flashcardId: String, val deletedAt: Long) : LibraryUiIntent
}
