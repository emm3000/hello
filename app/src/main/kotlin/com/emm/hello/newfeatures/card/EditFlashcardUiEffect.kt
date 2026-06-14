package com.emm.hello.newfeatures.card

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviEffect

sealed interface EditFlashcardUiEffect : MviEffect {
    data object NavigateBack : EditFlashcardUiEffect
    data object FlashcardDeleted : EditFlashcardUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : EditFlashcardUiEffect
}
