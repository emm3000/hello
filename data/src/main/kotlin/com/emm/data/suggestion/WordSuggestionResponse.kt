package com.emm.data.suggestion

import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.WordSuggestions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WordSuggestionResponseDto(
    val situation: String = "",
    val words: List<SuggestedWordDto> = emptyList(),
)

@Serializable
data class SuggestedWordDto(
    val word: String = "",
    val translation: String = "",
)

object WordSuggestionResponseParser {

    fun parse(raw: String, json: Json): WordSuggestions {
        val cleaned: String = stripJsonFences(raw)
        val dto: WordSuggestionResponseDto = json.decodeFromString<WordSuggestionResponseDto>(cleaned)
        check(dto.situation.isNotBlank()) { "Word suggestion response is missing a situation." }
        check(dto.words.isNotEmpty()) { "Word suggestion response has no words." }
        return WordSuggestions(
            situation = dto.situation,
            words = dto.words.map { SuggestedWord(word = it.word, translation = it.translation) },
        )
    }

    private fun stripJsonFences(raw: String): String {
        val trimmed: String = raw.trim()
        if (!trimmed.startsWith("```")) {
            return trimmed
        }
        return trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
