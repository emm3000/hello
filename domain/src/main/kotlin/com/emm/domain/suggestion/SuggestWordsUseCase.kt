package com.emm.domain.suggestion

import com.emm.domain.flashcard.FlashcardRepository

class SuggestWordsUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val suggestionRepository: WordSuggestionRepository,
) {

    suspend operator fun invoke(): WordSuggestions {
        val recentWords: List<String> = flashcardRepository.fetchRecentWords(RECENT_WORDS_LIMIT)
        val suggestions: WordSuggestions = suggestionRepository.suggest(recentWords)
        return suggestions.copy(words = SuggestedWordFilter.usable(suggestions.words, recentWords))
    }

    companion object {
        const val RECENT_WORDS_LIMIT: Int = 20
    }
}
