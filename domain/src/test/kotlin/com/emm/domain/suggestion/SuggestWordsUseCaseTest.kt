package com.emm.domain.suggestion

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestWordsUseCaseTest {

    @Test
    fun `invoke asks the flashcard repository for exactly the recent words limit`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = listOf("hello", "world"))
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        useCase()

        assertEquals(SuggestWordsUseCase.RECENT_WORDS_LIMIT, flashcardRepository.receivedLimit)
        assertEquals(listOf("hello", "world"), suggestionRepository.receivedRecentWords)
    }

    @Test
    fun `invoke drops a candidate already captured, case-insensitive`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = listOf("Serendipity"))
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(
                    SuggestedWord("serendipity", "casualidad"),
                    SuggestedWord("order", "pedir"),
                ),
            ),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        val result = useCase()

        assertEquals(listOf(SuggestedWord("order", "pedir")), result.words)
    }

    @Test
    fun `invoke collapses duplicate candidates to the first occurrence`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(
                    SuggestedWord("order", "pedir"),
                    SuggestedWord("Order", "ordenar"),
                ),
            ),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        val result = useCase()

        assertEquals(listOf(SuggestedWord("order", "pedir")), result.words)
    }

    @Test
    fun `invoke drops candidates with a blank word or translation`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(
                    SuggestedWord("", "pedir"),
                    SuggestedWord("order", ""),
                    SuggestedWord("table", "mesa"),
                ),
            ),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        val result = useCase()

        assertEquals(listOf(SuggestedWord("table", "mesa")), result.words)
    }

    @Test
    fun `invoke passes the situation through untouched`() = runBlocking {
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(situation = "Ordering food at a busy restaurant", words = emptyList()),
        )
        val useCase = SuggestWordsUseCase(flashcardRepository, suggestionRepository)

        val result = useCase()

        assertEquals("Ordering food at a busy restaurant", result.situation)
    }
}
