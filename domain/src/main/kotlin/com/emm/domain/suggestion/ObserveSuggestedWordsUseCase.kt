package com.emm.domain.suggestion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveSuggestedWordsUseCase(
    private val filter: SuggestedWordFilter,
    private val cache: WordSuggestionCache,
) {

    operator fun invoke(): Flow<WordSuggestions?> = cache.observe().map { cached ->
        cached?.let { suggestions -> suggestions.copy(words = filter.usable(suggestions.words)) }
    }
}
