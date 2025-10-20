package com.emm.domain.flashcard

class FlashcardCreator(
    private val repository: FlashcardRepository,
) {

    suspend fun createFlashcard(
        word: String,
        deckId: String,
        categories: StaticCategories,
        difficulty: String,
        typeView: TypeView,
    ): Flashcard {

        val flashcard: FlashcardGenerated = when(typeView) {
            TypeView.WordOrPhase -> repository.generateFlashcard(word)
            TypeView.WithCategories -> repository.generatedFlashcard(categories, difficulty)
        }
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