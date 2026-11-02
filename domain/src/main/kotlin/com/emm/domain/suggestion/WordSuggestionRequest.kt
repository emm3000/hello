package com.emm.domain.suggestion

data class WordSuggestionRequest(
    val recentWords: List<String>,
    val excludedSituations: List<String>,
    val excludedWords: List<String>,
)
