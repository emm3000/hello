package com.emm.domain.flashcard

class CreateFlashcardUseCase(
    private val repository: FlashcardRepository,
) {

    suspend operator fun invoke(
        deckId: String,
        flashcard: FlashcardGenerated,
    ): Flashcard {
        val input = CreateFlashcardInput(
            deckId = deckId,
            word = flashcard.word,
            meaning = flashcard.meaning,
            translation = flashcard.translation,
            phonetic = flashcard.phonetics,
            partOfSpeech = flashcard.partOfSpeech,
            type = flashcard.type,
            note = flashcard.notes,
        )

        val flashcardId: String = repository.create(input)

        repository.upsertExamples(flashcard.examples, flashcardId)

        return repository.fetchById(flashcardId)
    }
}
