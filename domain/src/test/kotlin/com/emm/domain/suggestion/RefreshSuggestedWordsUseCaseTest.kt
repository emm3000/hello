package com.emm.domain.suggestion

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshSuggestedWordsUseCaseTest {

    @Test
    fun `invoke asks the suggestion repository for the recent words`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(recentWords = listOf("hello", "world"))
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))),
        )
        val cache = FakeWordSuggestionCache()
        val useCase = RefreshSuggestedWordsUseCase(flashcardRepository, suggestionRepository, cache)

        useCase()

        assertEquals(SuggestWordsUseCase.RECENT_WORDS_LIMIT, flashcardRepository.receivedLimit)
        assertEquals(listOf("hello", "world"), suggestionRepository.receivedRecentWords)
    }

    @Test
    fun `invoke stores the filtered batch and preserves the situation`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(recentWords = listOf("Café"))
        val suggestionRepository = FakeWordSuggestionRepository(
            result = WordSuggestions(
                situation = "At a cafe",
                words = listOf(
                    SuggestedWord("cafe", "cafe"),
                    SuggestedWord("order", ""),
                    SuggestedWord("table", "mesa"),
                ),
            ),
        )
        val cache = FakeWordSuggestionCache()
        val useCase = RefreshSuggestedWordsUseCase(flashcardRepository, suggestionRepository, cache)

        useCase()

        assertEquals("At a cafe", cache.current?.situation)
        assertEquals(listOf(SuggestedWord("table", "mesa")), cache.current?.words)
    }

    @Test
    fun `invoke leaves the cache untouched when the suggestion repository throws`() = runTest {
        val failure = IllegalStateException("backend unavailable")
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val suggestionRepository = FakeWordSuggestionRepository(failure = failure)
        val cache = FakeWordSuggestionCache()
        val useCase = RefreshSuggestedWordsUseCase(flashcardRepository, suggestionRepository, cache)

        val thrown: Throwable? = runCatching { useCase() }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertNull(cache.current)
    }
}
