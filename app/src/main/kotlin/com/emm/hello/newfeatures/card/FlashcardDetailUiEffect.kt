package com.emm.hello.newfeatures.card

sealed interface FlashcardDetailUiEffect {
    data class LoadFailed(val message: String) : FlashcardDetailUiEffect
}
