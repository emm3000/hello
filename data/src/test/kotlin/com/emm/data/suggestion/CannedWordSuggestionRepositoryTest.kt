package com.emm.data.suggestion

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CannedWordSuggestionRepositoryTest {

    @Test
    fun `suggest returns 6 non-blank words and a non-blank situation`() = runTest {
        val repository = CannedWordSuggestionRepository(delayMs = 0L)

        val result = repository.suggest(recentWords = emptyList())

        assertTrue(result.situation.isNotBlank())
        assertEquals(6, result.words.size)
        result.words.forEach { word ->
            assertTrue(word.word.isNotBlank())
            assertTrue(word.translation.isNotBlank())
        }
    }

    @Test
    fun `suggest cycles through scenarios based on recent words count`() = runTest {
        val repository = CannedWordSuggestionRepository(delayMs = 0L)

        val first = repository.suggest(recentWords = emptyList())
        val second = repository.suggest(recentWords = List(1) { "word" })
        val third = repository.suggest(recentWords = List(2) { "word" })
        val fourth = repository.suggest(recentWords = List(3) { "word" })

        assertEquals(first.situation, fourth.situation)
        assertTrue(first.situation != second.situation)
        assertTrue(second.situation != third.situation)
    }
}
