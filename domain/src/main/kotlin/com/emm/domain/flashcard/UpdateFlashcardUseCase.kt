package com.emm.domain.flashcard

class UpdateFlashcardUseCase(
    private val flashcardRepository: FlashcardRepository,
) {

    suspend operator fun invoke(input: UpdateFlashcardInput) {
        require(input.word.isNotBlank()) { "Flashcard word must not be blank." }
        require(input.meaning.isNotBlank()) { "Flashcard meaning must not be blank." }

        flashcardRepository.updateFlashcard(input)
    }
}