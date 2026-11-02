package com.emm.domain.suggestion

import app.cash.turbine.test
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveSuggestedWordsUseCaseTest {

    @Test
    fun `invoke maps an empty cache to null`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(words = emptyList())
        val cache = FakeWordSuggestionCache()
        val useCase = ObserveSuggestedWordsUseCase(SuggestedWordFilter(flashcardRepository), cache)

        val emitted: WordSuggestions? = useCase().first()

        assertNull(emitted)
    }

    @Test
    fun `invoke removes already captured words from the cached batch`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(words = listOf("Café"))
        val cache = FakeWordSuggestionCache(
            initial = WordSuggestions(
                situation = "At a cafe",
                words = listOf(SuggestedWord("cafe", "cafe"), SuggestedWord("order", "pedir")),
            ),
        )
        val useCase = ObserveSuggestedWordsUseCase(SuggestedWordFilter(flashcardRepository), cache)

        val emitted: WordSuggestions? = useCase().first()

        assertEquals("At a cafe", emitted?.situation)
        assertEquals(listOf(SuggestedWord("order", "pedir")), emitted?.words)
    }

    @Test
    fun `invoke keeps the first seen time of the cached batch`() = runTest {
        val seenAt: Instant = Instant.parse("2026-11-02T08:00:00Z")
        val cache = FakeWordSuggestionCache(
            initial = WordSuggestions("At a cafe", listOf(SuggestedWord("order", "pedir")), firstSeenAt = seenAt),
        )
        val useCase = ObserveSuggestedWordsUseCase(SuggestedWordFilter(FakeFlashcardRepository(emptyList())), cache)

        val emitted: WordSuggestions? = useCase().first()

        assertEquals(Instant.parse("2026-11-02T08:00:00Z"), emitted?.firstSeenAt)
    }

    @Test
    fun `invoke re-filters a later emission against the current recent words`() = runTest {
        val flashcardRepository = FakeFlashcardRepository(words = emptyList())
        val cache = FakeWordSuggestionCache(
            initial = WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))),
        )
        val useCase = ObserveSuggestedWordsUseCase(SuggestedWordFilter(flashcardRepository), cache)

        useCase().test {
            assertEquals(listOf(SuggestedWord("order", "pedir")), awaitItem()?.words)

            flashcardRepository.words = listOf("Order")
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
