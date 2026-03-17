package com.emm.domain.quote

import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardWriteRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveQuoteAsFlashcardUseCaseTest {

    @Test
    fun `invoke when default deck is blank returns default deck required and does not create`() = runBlocking {
        val deckSelectionRepository = FakeDefaultDeckSelectionRepository(defaultDeckId = "")
        val flashcardWriteRepository = FakeFlashcardWriteRepository()
        val useCase = SaveQuoteAsFlashcardUseCase(
            getDefaultDeckUseCase = GetDefaultDeckUseCase(deckSelectionRepository),
            flashcardWriteRepository = flashcardWriteRepository,
        )

        val result = useCase(SaveQuoteAsFlashcardCommand.fromQuote(sampleQuote()))

        assertEquals(SaveQuoteAsFlashcardResult.DefaultDeckRequired, result)
        assertEquals(0, flashcardWriteRepository.createCalls)
        assertNull(flashcardWriteRepository.lastCreateInput)
    }

    @Test
    fun `invoke when default deck exists creates flashcard and returns saved`() = runBlocking {
        val deckSelectionRepository = FakeDefaultDeckSelectionRepository(defaultDeckId = "deck-1")
        val flashcardWriteRepository = FakeFlashcardWriteRepository()
        val useCase = SaveQuoteAsFlashcardUseCase(
            getDefaultDeckUseCase = GetDefaultDeckUseCase(deckSelectionRepository),
            flashcardWriteRepository = flashcardWriteRepository,
        )

        val result = useCase(SaveQuoteAsFlashcardCommand.fromQuote(sampleQuote()))

        assertEquals(SaveQuoteAsFlashcardResult.Saved, result)
        assertEquals(1, flashcardWriteRepository.createCalls)
        assertEquals(
            CreateFlashcardInput(
                id = "q-1",
                deckId = "deck-1",
                word = "break the ice",
                meaning = "Start a conversation in a social setting.",
                translation = "Romper el hielo",
                phonetic = "/breɪk ði aɪs/",
            ),
            flashcardWriteRepository.lastCreateInput,
        )
    }

    private fun sampleQuote(): Quote = Quote(
        id = "q-1",
        title = "Icebreaker",
        phrase = "break the ice",
        description = "Start a conversation in a social setting.",
        translation = "Romper el hielo",
        example = "He told a joke to break the ice.",
        context = "social",
        pronunciation = "/breɪk ði aɪs/",
        formality = "neutral",
        tags = listOf("idiom"),
        category = "conversation",
        hasCard = false,
    )

    private class FakeDefaultDeckSelectionRepository(
        private var defaultDeckId: String,
    ) : DefaultDeckSelectionRepository {
        override fun getDefaultDeckId(): String = defaultDeckId

        override fun setDefaultDeckId(deckId: String) {
            defaultDeckId = deckId
        }
    }

    private class FakeFlashcardWriteRepository : FlashcardWriteRepository {
        var createCalls: Int = 0
        var lastCreateInput: CreateFlashcardInput? = null

        override suspend fun create(input: CreateFlashcardInput): String {
            createCalls += 1
            lastCreateInput = input
            return input.id ?: "generated-id"
        }

        override suspend fun upsertExamples(examples: List<Example>, flashcardId: String) = Unit
    }
}
