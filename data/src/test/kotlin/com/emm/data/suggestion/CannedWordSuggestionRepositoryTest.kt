package com.emm.data.suggestion

import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.SuggestedWordFilter
import com.emm.domain.suggestion.WordSuggestionRequest
import com.emm.domain.suggestion.WordSuggestions
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CannedWordSuggestionRepositoryTest {

    private val repository = CannedWordSuggestionRepository(delayMs = 0L)

    @Test
    fun `every scenario has distinct situations and 3 single words plus 3 phrases with translations`() = runTest {
        val scenarios: List<WordSuggestions> = allScenarios()

        assertEquals(3, scenarios.size)
        assertEquals(3, scenarios.map { scenario -> scenario.situation }.distinct().size)
        scenarios.forEach { scenario ->
            val singleWords: List<SuggestedWord> = scenario.words.filterNot { item -> item.word.isPhrase() }
            val phrases: List<SuggestedWord> = scenario.words.filter { item -> item.word.isPhrase() }
            assertTrue(scenario.situation.isNotBlank())
            assertEquals(scenario.situation, 3, singleWords.size)
            assertEquals(scenario.situation, 3, phrases.size)
            scenario.words.forEach { item -> assertTrue(item.word, item.translation.isNotBlank()) }
        }
    }

    @Test
    fun `suggest skips a scenario whose situation is excluded`() = runTest {
        val first: WordSuggestions = repository.suggest(request(excludedSituations = emptyList()))

        val next: WordSuggestions = repository.suggest(request(excludedSituations = listOf(first.situation)))

        assertNotEquals(first.situation, next.situation)
    }

    @Test
    fun `with every canned item in history the variant keeps all its items after the filter`() = runTest {
        val scenarios: List<WordSuggestions> = allScenarios()
        val situations: List<String> = scenarios.map { scenario -> scenario.situation }
        val shownWords: MutableList<String> = scenarios.flatMap { scenario -> scenario.words }
            .map { item -> item.word }
            .toMutableList()
        val flashcardRepository: FlashcardRepository = mockk()
        coEvery { flashcardRepository.fetchAllWords() } returns emptyList()
        val filter = SuggestedWordFilter(flashcardRepository)

        val first: WordSuggestions = repository.suggest(request(excludedSituations = situations))
        val firstUsable: List<SuggestedWord> = filter.usable(first.words, shownWords)
        shownWords += first.words.map { item -> item.word }
        val second: WordSuggestions = repository.suggest(request(excludedSituations = situations + first.situation))
        val secondUsable: List<SuggestedWord> = filter.usable(second.words, shownWords)

        assertEquals(6, firstUsable.size)
        assertEquals(6, secondUsable.size)
        assertTrue(first.situation !in situations)
        assertNotEquals(first.situation, second.situation)
    }

    private suspend fun allScenarios(): List<WordSuggestions> {
        val collected: MutableList<WordSuggestions> = mutableListOf()
        repeat(MAX_SCENARIOS) {
            val excluded: List<String> = collected.map { scenario -> scenario.situation }.reversed()
            val next: WordSuggestions = repository.suggest(request(excludedSituations = excluded))
            if (next.situation in excluded || collected.any { next.situation.startsWith(it.situation) }) {
                return collected
            }
            collected += next
        }
        return collected
    }

    private fun request(excludedSituations: List<String>): WordSuggestionRequest = WordSuggestionRequest(
        recentWords = emptyList(),
        excludedSituations = excludedSituations,
        excludedWords = emptyList(),
    )

    private fun String.isPhrase(): Boolean = any { character -> character.isWhitespace() }

    private companion object {
        const val MAX_SCENARIOS: Int = 10
    }
}
