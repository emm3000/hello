package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.time.SystemClock

data class FlashcardDetailUiState(
    val flashcard: FlashcardDetail = FlashcardDetail(Flashcard.empty(SystemClock)),
)
