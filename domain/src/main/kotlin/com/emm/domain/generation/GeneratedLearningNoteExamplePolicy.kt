package com.emm.domain.generation

import com.emm.domain.text.lowercaseRoot
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class GeneratedLearningNoteExamplePolicy {

    fun collectIssues(note: GeneratedLearningNote): List<ValidationIssue.Error> {
        val example: String = note.exampleSentence.normalizeForMatch()
        if (example.isBlank()) return emptyList()

        val candidates: List<String> = buildList {
            add(note.expression.value)
            add(note.lemma)
            addAll(note.irregularForms)
        }.map { it.normalizeForMatch() }.filter { it.isNotBlank() }

        val exampleUsesExpression: Boolean = candidates.any { candidate -> example.uses(candidate) }
        return if (exampleUsesExpression) {
            emptyList()
        } else {
            listOf(
                ValidationIssue.Error(
                    code = IssueCode.ExampleDoesNotUseExpression,
                    field = "exampleSentence",
                ),
            )
        }
    }

    private fun String.uses(candidate: String): Boolean {
        val significantWords: List<String> = candidate
            .split(WHITESPACE)
            .filter { it.length >= MIN_MATCH_WORD_LENGTH }
        return if (significantWords.isEmpty()) contains(candidate) else significantWords.all { contains(it) }
    }

    private fun String.normalizeForMatch(): String {
        return lowercaseRoot()
            .replace('’', '\'')
            .replace(PUNCTUATION, " ")
            .trim()
            .replace(WHITESPACE, " ")
    }

    private companion object {
        const val MIN_MATCH_WORD_LENGTH = 3
        val WHITESPACE: Regex = "\\s+".toRegex()
        val PUNCTUATION: Regex = "[^\\p{L}\\p{N}' ]".toRegex()
    }
}
