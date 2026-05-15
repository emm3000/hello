package com.emm.domain.authoring

import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.ids.DeckId

class GeneratedLearningNoteMapper(
    private val artifactSerializer: LearningNoteArtifactSerializer,
) {

    fun toCreateFlashcardInput(
        deckId: DeckId,
        note: GeneratedLearningNote,
    ): CreateFlashcardInput {
        val encoded = artifactSerializer.encode(note.cards, note.qualityChecks)
        return CreateFlashcardInput(
            deckId = deckId,
            word = note.expression.value,
            meaning = note.simpleDefinitionEn.value,
            translation = note.intendedMeaningEs.value,
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
            studyCardsJson = encoded.studyCardsJson,
            qualityChecksJson = encoded.qualityChecksJson,
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
