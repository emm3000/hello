package com.emm.domain.generation

import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.LearningNoteType
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.text.lowercaseRoot
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class GeneratedLearningNoteTypeRequirementsPolicy {

    fun collectIssues(note: GeneratedLearningNote): List<ValidationIssue.Error> {
        val errors = mutableListOf<ValidationIssue.Error>()

        when (note.noteType) {
            LearningNoteType.Word -> {
                requireExpectedCard(note, StudyCardType.Recognition, errors)
                requireExpectedCard(note, StudyCardType.Production, errors)
            }

            LearningNoteType.Phrase -> {
                requireUsagePattern(note, errors)
                requireExpectedCard(note, StudyCardType.Recognition, errors)
                requireExpectedCard(note, StudyCardType.Production, errors)
                requireExpectedCard(note, StudyCardType.Cloze, errors)
            }

            LearningNoteType.PhrasalVerb -> {
                requireUsagePattern(note, errors)
                requireExpectedCard(note, StudyCardType.Recognition, errors)
                requireExpectedCard(note, StudyCardType.Production, errors)
                requireExpectedCard(note, StudyCardType.Cloze, errors)
            }

            LearningNoteType.Idiom -> {
                requireExpectedCard(note, StudyCardType.Recognition, errors)
                requireExpectedCard(note, StudyCardType.Production, errors)
            }

            LearningNoteType.SentencePattern -> {
                requireUsagePattern(note, errors)
                requireNonBlank(
                    value = note.clozeSentence,
                    code = IssueCode.MissingClozeSentence,
                    errors = errors,
                    field = "clozeSentence",
                )
                requireExpectedCard(note, StudyCardType.Production, errors)
                requireExpectedCard(note, StudyCardType.Cloze, errors)
            }
        }

        return errors
    }

    private fun requireUsagePattern(
        note: GeneratedLearningNote,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        requireNonBlank(
            value = note.usagePattern,
            code = IssueCode.MissingUsagePattern,
            errors = errors,
            field = "usagePattern",
        )
    }

    private fun requireExpectedCard(
        note: GeneratedLearningNote,
        cardType: StudyCardType,
        errors: MutableList<ValidationIssue.Error>,
    ) {
        val hasCard = note.cards.any { it.cardType == cardType && it.isActive }
        if (!hasCard) {
            errors += ValidationIssue.Error(
                code = IssueCode.MissingExpectedCardType,
                field = "cards.expected.${cardType.name.lowercaseRoot()}",
            )
        }
    }

    private fun requireNonBlank(
        value: String,
        code: IssueCode,
        errors: MutableList<ValidationIssue.Error>,
        field: String,
    ) {
        if (value.isBlank()) {
            errors += ValidationIssue.Error(code = code, field = field)
        }
    }
}
