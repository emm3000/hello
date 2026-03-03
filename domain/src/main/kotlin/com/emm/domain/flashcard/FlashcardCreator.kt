package com.emm.domain.flashcard

class FlashcardCreator(
    private val repository: FlashcardRepository,
) {

    suspend fun generateFlashcardPreview(
        word: String,
        categories: StaticCategories,
        difficulty: String,
        typeView: TypeView,
    ): FlashcardGenerated {
        return when (typeView) {
            TypeView.WordOrPhase -> repository.generateFlashcard(word)
            TypeView.WithCategories -> repository.generatedFlashcard(categories, difficulty)
        }
    }

    suspend fun saveFlashcard(
        deckId: String,
        flashcard: FlashcardGenerated,
    ): Flashcard {
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
