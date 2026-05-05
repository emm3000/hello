package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot
import com.emm.domain.validation.IssueCode
import com.emm.domain.validation.ValidationIssue

class GeneratedLearningNoteQualityChecksPolicy {

    fun collectIssues(note: GeneratedLearningNote): List<ValidationIssue.Error> {
        val errors = mutableListOf<ValidationIssue.Error>()
        val presentCodes = note.qualityChecks.map { it.code }.toSet()

        if (!presentCodes.contains(GeneratedNoteQualityCode.SingleMeaning)) {
            errors += ValidationIssue.Error(
                code = IssueCode.MissingSingleMeaningQualityCheck,
                field = "qualityChecks.singleMeaning",
            )
        }

        GeneratedNoteQualityCode.entries
            .filterNot(presentCodes::contains)
            .forEach { missingCode ->
                errors += ValidationIssue.Error(
                    code = IssueCode.MissingRequiredQualityCheck,
                    field = "qualityChecks.${missingCode.name.lowercaseRoot()}",
                )
            }

        val failedChecks = note.qualityChecks.filterNot { it.passed }
        if (failedChecks.isNotEmpty()) {
            errors += ValidationIssue.Error(
                code = IssueCode.FailedQualityCheck,
                field = "qualityChecks.failed",
            )
        }

        return errors
    }
}
