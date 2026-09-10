package com.emm.domain.suggestion

import com.emm.domain.text.searchNormalized

object SuggestedWordFilter {

    fun usable(candidates: List<SuggestedWord>, capturedWords: List<String>): List<SuggestedWord> {
        val captured: Set<String> = capturedWords.map { word -> word.searchNormalized() }.toSet()
        val seenWords: MutableSet<String> = mutableSetOf()
        val filtered: MutableList<SuggestedWord> = mutableListOf()
        candidates.forEach { candidate ->
            val normalizedWord: String = candidate.word.searchNormalized()
            val isUsable: Boolean = candidate.word.isNotBlank() &&
                candidate.translation.isNotBlank() &&
                normalizedWord !in captured &&
                normalizedWord !in seenWords
            if (isUsable) {
                seenWords += normalizedWord
                filtered += candidate
            }
        }
        return filtered
    }
}
