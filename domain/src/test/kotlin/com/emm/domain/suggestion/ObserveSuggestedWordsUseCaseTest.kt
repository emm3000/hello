package com.emm.domain.suggestion

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveSuggestedWordsUseCaseTest {

    @Test
    fun `invoke maps an empty cache to null`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val cache = FakeWordSuggestionCache()
        val useCase = ObserveSuggestedWordsUseCase(flashcardRepository, cache)

        val emitted: WordSuggestions? = useCase().first()

        assertNull(emitted)
    }

    @Test
    fun `invoke removes already captured words from the cached batch`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(recentWords = listOf("Café"))
        val cache = FakeWordSuggestionCache(
            initial = WordSuggestions(
                situation = "At a cafe",
                words = listOf(SuggestedWord("cafe", "cafe"), SuggestedWord("order", "pedir")),
            ),
        )
        val useCase = ObserveSuggestedWordsUseCase(flashcardRepository, cache)

        val emitted: WordSuggestions? = useCase().first()

        assertEquals("At a cafe", emitted?.situation)
        assertEquals(listOf(SuggestedWord("order", "pedir")), emitted?.words)
    }

    @Test
    fun `invoke re-filters a later emission against the current recent words`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(recentWords = emptyList())
        val cache = FakeWordSuggestionCache(
            initial = WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))),
        )
        val useCase = ObserveSuggestedWordsUseCase(flashcardRepository, cache)

        useCase().test {
            assertEquals(listOf(SuggestedWord("order", "pedir")), awaitItem()?.words)

            flashcardRepository.recentWords = listOf("Order")
            cache.emit(
                WordSuggestions(
                    situation = "At a cafe",
                    words = listOf(SuggestedWord("order", "pedir"), SuggestedWord("table", "mesa")),
                ),
            )

            assertEquals(listOf(SuggestedWord("table", "mesa")), awaitItem()?.words)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
