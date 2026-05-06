package com.emm.domain.authoring

import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.ids.DeckId

class GeneratedLearningNoteMapper {

    fun toCreateFlashcardInput(
        deckId: DeckId,
        note: GeneratedLearningNote,
    ): CreateFlashcardInput {
        val expression = note.requireExpression()
        val intendedMeaningEs = note.requireIntendedMeaningEs()
        val definitionEn = note.requireSimpleDefinitionEn()

        return CreateFlashcardInput(
            deckId = deckId,
            word = expression.value,
            meaning = definitionEn.value,
            translation = intendedMeaningEs.value,
            phonetic = note.ipa,
            partOfSpeech = note.partOfSpeech.name,
            noteType = note.noteType.name,
            noteSummary = buildNoteSummary(note),
            register = note.register.name,
            levelBand = note.levelBand.name,
            learningDomain = note.domain.name,
            lemma = note.lemma,
            whyUseful = note.whyUseful,
            usagePattern = note.usagePattern,
            irregularForms = note.irregularForms,
            collocations = note.collocations,
            commonMistake = note.commonMistake,
            confusableWith = note.confusableWith,
            clozeSentence = note.clozeSentence,
            sourceContext = note.sourceContext,
            warnings = note.warnings,
            studyCards = note.cards,
            qualityChecks = note.qualityChecks,
        )
    }

    fun toExamples(note: GeneratedLearningNote): List<Example> {
        return buildList {
            if (note.exampleSentence.isNotBlank()) {
                add(
                    Example(
                        exampleId = "learning-note-example",
                        text = note.exampleSentence,
                        translation = note.exampleTranslation,
                        type = "main",
                    )
                )
            }
        }
    }

    private fun buildNoteSummary(note: GeneratedLearningNote): String {
        return listOfNotNull(
            note.whyUseful.takeIf { it.isNotBlank() },
            note.usagePattern.takeIf { it.isNotBlank() },
            note.commonMistake.takeIf { it.isNotBlank() },
        ).joinToString(separator = " | ")
    }
}
