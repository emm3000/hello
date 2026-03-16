package com.emm.domain.quote

import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.FlashcardRepository

class SaveQuoteAsFlashcardUseCase(
    private val getDefaultDeckUseCase: GetDefaultDeckUseCase,
    private val flashcardRepository: FlashcardRepository,
) {
    suspend fun execute(quote: Quote): SaveQuoteAsFlashcardResult {
        val defaultDeckId = getDefaultDeckUseCase.execute()
        if (defaultDeckId.isBlank()) {
            return SaveQuoteAsFlashcardResult.DefaultDeckRequired
        }

        flashcardRepository.create(
            CreateFlashcardInput(
                id = quote.id,
                deckId = defaultDeckId,
                word = quote.phrase,
                meaning = quote.description,
                translation = quote.translation,
                phonetic = quote.pronunciation,
            )
        )

        return SaveQuoteAsFlashcardResult.Saved
    }
}

enum class SaveQuoteAsFlashcardResult {
    Saved,
    DefaultDeckRequired,
}
