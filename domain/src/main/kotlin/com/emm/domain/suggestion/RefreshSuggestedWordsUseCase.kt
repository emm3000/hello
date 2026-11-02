package com.emm.domain.suggestion

import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.time.Clock
import java.time.Duration

class RefreshSuggestedWordsUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val suggestionRepository: WordSuggestionRepository,
    private val cache: WordSuggestionCache,
    private val historyRepository: SuggestionHistoryRepository,
    private val filter: SuggestedWordFilter,
    private val clock: Clock,
) {

    suspend operator fun invoke(): WordSuggestions {
        historyRepository.deleteShownBefore(clock.now().minus(HISTORY_RETENTION))
        val history: SuggestionHistory = historyRepository.fetch()
        val suggestions: WordSuggestions = suggestionRepository.suggest(buildRequest(history))
        val stored: WordSuggestions = suggestions.copy(words = filter.usable(suggestions.words, history.words))
        cache.replace(stored)
        return stored
    }

    private suspend fun buildRequest(history: SuggestionHistory): WordSuggestionRequest = WordSuggestionRequest(
        recentWords = flashcardRepository.fetchRecentWords(SuggestedWordFilter.RECENT_WORDS_LIMIT),
        excludedSituations = history.situations.distinct().take(EXCLUDED_SITUATIONS_LIMIT),
        excludedWords = history.words.distinct().take(EXCLUDED_WORDS_LIMIT),
    )

    companion object {
        const val EXCLUDED_SITUATIONS_LIMIT: Int = 30
        const val EXCLUDED_WORDS_LIMIT: Int = 120
        val HISTORY_RETENTION: Duration = Duration.ofDays(60)
    }
}
