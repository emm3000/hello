package com.emm.domain.flashcard

class CreateFlashcardUseCase(
    private val writeRepository: FlashcardWriteRepository,
    private val readRepository: FlashcardReadRepository,
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

        val flashcardId: String = writeRepository.create(input)

        writeRepository.upsertExamples(flashcard.examples, flashcardId)

        return readRepository.fetchById(flashcardId)
    }
}
