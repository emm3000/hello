package com.emm.data.suggestion

import com.emm.data.flashcard.GeminiService
import com.emm.domain.suggestion.WordSuggestionRepository
import com.emm.domain.suggestion.WordSuggestions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GeminiWordSuggestionRepository(
    private val geminiService: GeminiService,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) : WordSuggestionRepository {

    override suspend fun suggest(recentWords: List<String>): WordSuggestions = withContext(ioDispatcher) {
        val prompt: String = WordSuggestionPrompt.build(recentWords)
        val raw: String = geminiService.process(prompt)
        WordSuggestionResponseParser.parse(raw, json)
    }
}
