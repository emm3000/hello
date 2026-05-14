package com.emm.hello.newfeatures.card

import com.emm.hello.core.mvi.MviEffect

sealed interface EditFlashcardUiEffect : MviEffect {
    data object NavigateBack : EditFlashcardUiEffect
    data class ShowMessage(val message: String) : EditFlashcardUiEffect
}
