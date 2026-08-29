package com.emm.domain.suggestion

interface WordSuggestionRepository {
    suspend fun suggest(recentWords: List<String>): WordSuggestions
}
