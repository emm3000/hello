package com.emm.domain.flashcard

import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import com.emm.domain.text.lowercaseRoot

class ExactDuplicateKey private constructor(
    val deckId: DeckId,
    val expression: Expression,
    val intendedMeaningEs: IntendedMeaningEs,
    val noteType: LearningNoteType,
) {

    val canonicalValue: String
        get() = listOf(
            deckId.value,
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
            val normalizedDeckId = deckId.toDeckId()
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
