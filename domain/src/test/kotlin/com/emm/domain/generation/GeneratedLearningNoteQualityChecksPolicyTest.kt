package com.emm.domain.generation

import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertTrue

class GeneratedLearningNoteQualityChecksPolicyTest {

    private val policy = GeneratedLearningNoteQualityChecksPolicy()

    @Test
    fun `collectIssues returns missing single meaning error`() {
        val note = sampleWordNote().copy(
            qualityChecks = listOf(
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.NaturalExample,
                    passed = true,
                    message = "ok",
                )
            )
        )

        val issues = policy.collectIssues(note)

        assertTrue(issues.any { it.code == IssueCode.MissingSingleMeaningQualityCheck })
        assertTrue(issues.any { it.code == IssueCode.MissingRequiredQualityCheck })
    }

    @Test
    fun `collectIssues returns failed quality check error when any check fails`() {
        val note = sampleWordNote().copy(
            qualityChecks = fullQualityChecks().map { check ->
                if (check.code == GeneratedNoteQualityCode.NonAmbiguousAnswers) {
                    check.copy(passed = false, message = "ambigua")
                } else {
                    check
                }
            }
        )

        val issues = policy.collectIssues(note)

        assertTrue(issues.any { it.code == IssueCode.FailedQualityCheck })
    }

    private fun sampleWordNote(): GeneratedLearningNote {
        return GeneratedLearningNote(
            noteId = "note-1",
            noteType = LearningNoteType.Word,
            expression = "borrow".toExpression(),
            intendedMeaningEs = "pedir prestado".toIntendedMeaningEs(),
            simpleDefinitionEn = "to take something and return it later".toDefinitionEn(),
            partOfSpeech = PartOfSpeechTag.Verb,
            register = RegisterPreference.Neutral,
            levelBand = LevelBand.A1_A2,
            domain = LearningDomain.DailyLife,
            whyUseful = "Sirve para hablar de prestamos.",
            exampleSentence = "Can I borrow your pen?",
            exampleTranslation = "Puedo pedirte prestado tu lapicero?",
            cards = listOf(
                GeneratedStudyCard(
                    cardId = "card-1",
                    cardType = StudyCardType.Recognition,
                    prompt = "borrow",
                    expectedAnswer = "pedir prestado",
                    evaluationMode = EvaluationMode.FlexibleText,
                )
            ),
            qualityChecks = fullQualityChecks(),
        )
    }

    private fun fullQualityChecks(): List<GeneratedNoteQualityCheck> {
        return listOf(
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.SingleMeaning,
                passed = true,
                message = "ok",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.NaturalExample,
                passed = true,
                message = "ok",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.ExampleSupportsMeaning,
                passed = true,
                message = "ok",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.NonAmbiguousAnswers,
                passed = true,
                message = "ok",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.RequiredFieldsPresent,
                passed = true,
                message = "ok",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.ClearCardFocus,
                passed = true,
                message = "ok",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.NoteCardAlignment,
                passed = true,
                message = "ok",
            ),
        )
    }
}
