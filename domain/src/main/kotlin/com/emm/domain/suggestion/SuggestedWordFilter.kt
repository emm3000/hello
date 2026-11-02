package com.emm.domain.suggestion

import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.text.searchNormalized

class SuggestedWordFilter(private val flashcardRepository: FlashcardRepository) {

    suspend fun usable(candidates: List<SuggestedWord>, shownWords: List<String> = emptyList()): List<SuggestedWord> {
        val excluded: Set<String> = (flashcardRepository.fetchAllWords() + shownWords)
            .map { word -> word.searchNormalized() }
            .toSet()
        val seenWords: MutableSet<String> = mutableSetOf()
        val filtered: MutableList<SuggestedWord> = mutableListOf()
        candidates.forEach { candidate ->
            val normalizedWord: String = candidate.word.searchNormalized()
            val isUsable: Boolean = candidate.word.isNotBlank() &&
                candidate.translation.isNotBlank() &&
                normalizedWord !in excluded &&
                normalizedWord !in seenWords
            if (isUsable) {
                seenWords += normalizedWord
                filtered += candidate
            }
        }
        return filtered
    }

    companion object {
        const val RECENT_WORDS_LIMIT: Int = 20
    }
}
