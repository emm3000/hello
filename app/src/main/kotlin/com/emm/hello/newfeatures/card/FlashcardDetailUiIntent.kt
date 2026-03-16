package com.emm.hello.newfeatures.card

sealed interface FlashcardDetailUiIntent {
    data object Load : FlashcardDetailUiIntent
}
