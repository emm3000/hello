package com.emm.domain.flashcard

class FlashcardCreator(private val repository: FlashcardRepository) {

    suspend fun createFlashcard(word: String, deckId: String): Flashcard {

        val flashcard: FlashcardGenerated = repository.generateFlashcard(word)
        val input = CreateFlashcardInput(
            deckId = deckId,
            word = flashcard.word,
            meaning = flashcard.meaning,
            translation = flashcard.translation,
            phonetic = flashcard.phonetics,
        )

        val flashcardId: String = repository.create(input)

        repository.upsertExamples(flashcard.examples, flashcardId)

        return repository.fetchById(flashcardId)
    }
}