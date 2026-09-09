package com.emm.hello.newfeatures.card

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface FlashcardDetailUiEffect : MviEffect {
    data class LoadFailed(@StringRes val messageRes: Int) : FlashcardDetailUiEffect
    data object NavigateBack : FlashcardDetailUiEffect
    data class NavigateToEditFlashcard(val cardId: String) : FlashcardDetailUiEffect
    data object FlashcardDeleted : FlashcardDetailUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : FlashcardDetailUiEffect
}
