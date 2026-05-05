package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.time.SystemClock

data class FlashcardDetailUiState(
    val flashcard: Flashcard = Flashcard.empty(SystemClock),
)
