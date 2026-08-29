package com.emm.domain.suggestion

import com.emm.domain.flashcard.FlashcardRepository

class SuggestWordsUseCase(
    private val flashcardRepository: FlashcardRepository,
    private val suggestionRepository: WordSuggestionRepository,
) {

    suspend operator fun invoke(): WordSuggestions {
        val recentWords: List<String> = flashcardRepository.fetchRecentWords(RECENT_WORDS_LIMIT)
        val suggestions: WordSuggestions = suggestionRepository.suggest(recentWords)
        val capturedWords: Set<String> = recentWords.map { it.normalized() }.toSet()
        return suggestions.copy(words = filterCandidates(suggestions.words, capturedWords))
    }

    private fun filterCandidates(
        candidates: List<SuggestedWord>,
        capturedWords: Set<String>,
    ): List<SuggestedWord> {
        val seenWords: MutableSet<String> = mutableSetOf()
        val filtered: MutableList<SuggestedWord> = mutableListOf()
        candidates.forEach { candidate ->
            val normalizedWord: String = candidate.word.normalized()
            val isUsable: Boolean = candidate.word.isNotBlank() &&
                candidate.translation.isNotBlank() &&
                normalizedWord !in capturedWords &&
                normalizedWord !in seenWords
            if (isUsable) {
                seenWords += normalizedWord
                filtered += candidate
            }
        }
        return filtered
    }

    private fun String.normalized(): String = trim().lowercase()

    companion object {
        const val RECENT_WORDS_LIMIT: Int = 20
    }
}
