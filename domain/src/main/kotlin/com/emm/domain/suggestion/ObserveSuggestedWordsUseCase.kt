package com.emm.domain.suggestion

import com.emm.domain.flashcard.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveSuggestedWordsUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val cache: WordSuggestionCache,
) {

    operator fun invoke(): Flow<WordSuggestions?> = cache.observe().map { cached ->
        cached?.let { suggestions ->
            val recentWords: List<String> =
                flashcardRepository.fetchRecentWords(SuggestWordsUseCase.RECENT_WORDS_LIMIT)
            suggestions.copy(words = SuggestedWordFilter.usable(suggestions.words, recentWords))
        }
    }
}
