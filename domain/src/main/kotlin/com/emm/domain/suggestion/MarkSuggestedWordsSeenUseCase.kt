package com.emm.domain.suggestion

class MarkSuggestedWordsSeenUseCase(private val cache: WordSuggestionCache) {

    suspend operator fun invoke() {
        cache.markSeen()
    }
}
