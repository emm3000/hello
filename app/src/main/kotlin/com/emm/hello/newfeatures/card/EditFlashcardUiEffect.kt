package com.emm.hello.newfeatures.card

sealed interface EditFlashcardUiEffect {
    data object NavigateBack : EditFlashcardUiEffect
    data class ShowMessage(val message: String) : EditFlashcardUiEffect
}