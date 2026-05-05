package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot

class ExactDuplicateKey private constructor(
    val deckId: String,
    val expression: Expression,
    val intendedMeaningEs: IntendedMeaningEs,
    val noteType: LearningNoteType,
) {

    val canonicalValue: String
        get() = listOf(
            deckId,
            expression.canonical,
            intendedMeaningEs.canonical,
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
            val normalizedExpression = expression.toExpression()
            val normalizedMeaning = intendedMeaningEs.toIntendedMeaningEs()

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
