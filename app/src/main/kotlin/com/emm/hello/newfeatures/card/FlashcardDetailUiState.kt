package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.Flashcard

data class FlashcardDetailUiState(
    val flashcard: Flashcard = Flashcard.Empty,
)
