package com.emm.domain.suggestion

import com.emm.domain.flashcard.FlashcardRepository

class RefreshSuggestedWordsUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val suggestionRepository: WordSuggestionRepository,
    private val cache: WordSuggestionCache,
) {

    suspend operator fun invoke() {
        val recentWords: List<String> =
            flashcardRepository.fetchRecentWords(SuggestedWordFilter.RECENT_WORDS_LIMIT)
        val suggestions: WordSuggestions = suggestionRepository.suggest(recentWords)
        cache.replace(suggestions.copy(words = SuggestedWordFilter.usable(suggestions.words, recentWords)))
    }
}
