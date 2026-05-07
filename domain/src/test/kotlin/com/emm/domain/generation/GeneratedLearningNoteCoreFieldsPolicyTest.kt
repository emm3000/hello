package com.emm.domain.generation

import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertTrue

class GeneratedLearningNoteCoreFieldsPolicyTest {

    private val policy = GeneratedLearningNoteCoreFieldsPolicy()

    @Test
    fun `collectIssues returns missing note id error`() {
        val issues = policy.collectIssues(
            sampleWordNote().copy(
                noteId = "",
            )
        )

        assertTrue(issues.any { it.code == IssueCode.MissingNoteId })
    }

    @Test
    fun `collectIssues returns missing cards and quality checks`() {
        val issues = policy.collectIssues(
            sampleWordNote().copy(
                cards = emptyList(),
                qualityChecks = emptyList(),
            )
        )

        assertTrue(issues.any { it.code == IssueCode.MissingCards })
        assertTrue(issues.any { it.code == IssueCode.MissingQualityChecks })
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
                    cardId = "card-rec",
                    cardType = StudyCardType.Recognition,
                    prompt = "borrow",
                    expectedAnswer = "pedir prestado",
                    evaluationMode = EvaluationMode.FlexibleText,
                )
            ),
            qualityChecks = listOf(
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.SingleMeaning,
                    passed = true,
                    message = "ok",
                )
            ),
        )
    }
}
