package com.emm.hello.newfeatures.card

sealed interface FlashcardDetailUiIntent {
    data object Load : FlashcardDetailUiIntent
    data object EditFlashcard : FlashcardDetailUiIntent
    data object DeleteFlashcard : FlashcardDetailUiIntent
    data object ConfirmDeleteFlashcard : FlashcardDetailUiIntent
    data object DismissDeleteFlashcard : FlashcardDetailUiIntent
}