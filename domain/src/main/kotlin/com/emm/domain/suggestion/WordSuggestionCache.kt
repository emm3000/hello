package com.emm.domain.suggestion

import kotlinx.coroutines.flow.Flow

interface WordSuggestionCache {

    fun observe(): Flow<WordSuggestions?>

    suspend fun replace(suggestions: WordSuggestions)
}
