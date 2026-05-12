package com.emm.hello.newfeatures.card

sealed interface FlashcardDetailUiEffect {
    data class LoadFailed(val message: String) : FlashcardDetailUiEffect
    data class NavigateToEditFlashcard(val cardId: String) : FlashcardDetailUiEffect
    data object FlashcardDeleted : FlashcardDetailUiEffect
    data class ShowMessage(val message: String) : FlashcardDetailUiEffect
}