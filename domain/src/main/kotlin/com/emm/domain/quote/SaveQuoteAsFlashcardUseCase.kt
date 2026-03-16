package com.emm.domain.quote

import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.FlashcardWriteRepository

class SaveQuoteAsFlashcardUseCase(
    private val getDefaultDeckUseCase: GetDefaultDeckUseCase,
    private val flashcardWriteRepository: FlashcardWriteRepository,
) {
    suspend operator fun invoke(quote: Quote): SaveQuoteAsFlashcardResult {
        return invoke(SaveQuoteAsFlashcardCommand.fromQuote(quote))
    }

    suspend operator fun invoke(command: SaveQuoteAsFlashcardCommand): SaveQuoteAsFlashcardResult {
        val defaultDeckId = getDefaultDeckUseCase()
        if (defaultDeckId.isBlank()) {
            return SaveQuoteAsFlashcardResult.DefaultDeckRequired
        }

        flashcardWriteRepository.create(command.toCreateFlashcardInput(defaultDeckId))

        return SaveQuoteAsFlashcardResult.Saved
    }

    private fun SaveQuoteAsFlashcardCommand.toCreateFlashcardInput(defaultDeckId: String): CreateFlashcardInput {
        return CreateFlashcardInput(
            id = quoteId,
            deckId = defaultDeckId,
            word = phrase,
            meaning = description,
            translation = translation,
            phonetic = pronunciation,
        )
    }
}

enum class SaveQuoteAsFlashcardResult {
    Saved,
    DefaultDeckRequired,
}
