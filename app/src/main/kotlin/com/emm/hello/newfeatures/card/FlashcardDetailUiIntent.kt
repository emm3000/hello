package com.emm.hello.newfeatures.card

import com.emm.hello.core.mvi.MviIntent

sealed interface FlashcardDetailUiIntent : MviIntent {
    data object Load : FlashcardDetailUiIntent
    data object EditFlashcard : FlashcardDetailUiIntent
    data object DeleteFlashcard : FlashcardDetailUiIntent
    data object ConfirmDeleteFlashcard : FlashcardDetailUiIntent
    data object DismissDeleteFlashcard : FlashcardDetailUiIntent
}
