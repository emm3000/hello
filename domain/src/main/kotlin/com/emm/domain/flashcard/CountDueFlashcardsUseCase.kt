package com.emm.domain.flashcard

import com.emm.domain.time.Clock

class CountDueFlashcardsUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(): Long {
        return flashcardRepository.countDueFlashcards(nowMillis = clock.now().toEpochMilli())
    }
}
