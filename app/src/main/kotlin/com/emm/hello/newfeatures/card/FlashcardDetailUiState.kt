package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.time.SystemClock
import com.emm.hello.core.mvi.MviState

data class FlashcardDetailUiState(
    val flashcard: Flashcard = Flashcard.empty(SystemClock),
    val isLoading: Boolean = true,
    val isDeleteConfirmationVisible: Boolean = false,
) : MviState
