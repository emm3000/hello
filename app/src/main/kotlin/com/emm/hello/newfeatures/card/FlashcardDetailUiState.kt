package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.time.SystemClock

data class FlashcardDetailUiState(
    val flashcard: Flashcard = Flashcard(
        id = "",
        word = "",
        meaning = "",
        translation = "",
        examples = emptyList(),
        phonetic = "",
        review = FlashcardReview.empty(SystemClock),
    ),
)
