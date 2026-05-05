package com.emm.domain.flashcard

class GeneratedLearningNoteMapper {

    fun toCreateFlashcardInput(
        deckId: String,
        note: GeneratedLearningNote,
    ): CreateFlashcardInput {
        val expression = note.expression.toExpression()
        val intendedMeaningEs = note.intendedMeaningEs.toIntendedMeaningEs()
        val definitionEn = note.simpleDefinitionEn.toDefinitionEn()

        return CreateFlashcardInput(
            deckId = deckId,
            word = expression.value,
            meaning = definitionEn.value,
            translation = intendedMeaningEs.value,
            phonetic = note.ipa,
            partOfSpeech = note.partOfSpeech.name,
            type = note.noteType.name,
            note = buildNoteSummary(note),
            register = note.register.name,
            levelBand = note.levelBand.name,
            domain = note.domain.name,
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
