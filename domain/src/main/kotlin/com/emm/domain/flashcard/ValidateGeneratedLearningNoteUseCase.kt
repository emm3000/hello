package com.emm.domain.flashcard

import com.emm.domain.validation.ValidationIssue
import com.emm.domain.validation.ValidationResult

class ValidateGeneratedLearningNoteUseCase(
    private val coreFieldsPolicy: GeneratedLearningNoteCoreFieldsPolicy =
        GeneratedLearningNoteCoreFieldsPolicy(),
    private val typeRequirementsPolicy: GeneratedLearningNoteTypeRequirementsPolicy =
        GeneratedLearningNoteTypeRequirementsPolicy(),
    private val cardsPolicy: GeneratedLearningNoteCardsPolicy = GeneratedLearningNoteCardsPolicy(),
    private val qualityChecksPolicy: GeneratedLearningNoteQualityChecksPolicy =
        GeneratedLearningNoteQualityChecksPolicy(),
) {

    operator fun invoke(note: GeneratedLearningNote): ValidationResult<GeneratedLearningNote> {
        val errors = mutableListOf<ValidationIssue.Error>()
        val warnings = mutableListOf<ValidationIssue.Warning>()

        errors += coreFieldsPolicy.collectIssues(note)
        errors += typeRequirementsPolicy.collectIssues(note)
        val cardsValidation = cardsPolicy.collectIssues(note)
        errors += cardsValidation.errors
        warnings += cardsValidation.warnings
        errors += qualityChecksPolicy.collectIssues(note)

        return if (errors.isEmpty()) {
            ValidationResult.valid(value = note, warnings = warnings)
        } else {
            ValidationResult.invalid(value = note, errors = errors, warnings = warnings)
        }
    }

}
