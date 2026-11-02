package com.emm.domain.suggestion

interface WordSuggestionRepository {
    suspend fun suggest(request: WordSuggestionRequest): WordSuggestions
}
