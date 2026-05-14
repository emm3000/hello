package com.emm.hello.newfeatures.card

import com.emm.hello.core.mvi.MviEffect

sealed interface FlashcardDetailUiEffect : MviEffect {
    data class LoadFailed(val message: String) : FlashcardDetailUiEffect
    data class NavigateToEditFlashcard(val cardId: String) : FlashcardDetailUiEffect
    data object FlashcardDeleted : FlashcardDetailUiEffect
    data class ShowMessage(val message: String) : FlashcardDetailUiEffect
}
