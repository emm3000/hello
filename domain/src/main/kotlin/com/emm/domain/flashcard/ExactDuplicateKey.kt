package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot

class ExactDuplicateKey private constructor(
    val deckId: String,
    val expression: String,
    val intendedMeaningEs: String,
    val noteType: LearningNoteType,
) {

    val canonicalValue: String
        get() = listOf(
            deckId,
            expression,
            intendedMeaningEs,
            noteType.name.lowercaseRoot(),
        ).joinToString(separator = "|")

    companion object {
        fun from(
            deckId: String,
            expression: String,
            intendedMeaningEs: String,
            noteType: LearningNoteType,
        ): ExactDuplicateKey {
            val normalizedDeckId = deckId.normalizeDeckId()
            val normalizedExpression = expression.normalizeTextField(fieldName = "expression")
            val normalizedMeaning = intendedMeaningEs.normalizeTextField(fieldName = "intendedMeaningEs")

            return ExactDuplicateKey(
                deckId = normalizedDeckId,
                expression = normalizedExpression,
                intendedMeaningEs = normalizedMeaning,
                noteType = noteType,
            )
        }
    }
}

private fun String.normalizeDeckId(): String {
    val normalized = trim()
    require(normalized.isNotEmpty()) { "deckId cannot be blank." }
    return normalized
}

private fun String.normalizeTextField(fieldName: String): String {
    val normalized = trim().replace("\\s+".toRegex(), " ").lowercaseRoot()
    require(normalized.isNotEmpty()) { "$fieldName cannot be blank." }
    return normalized
}
