package com.emm.domain.generation

import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.StudyCardType
import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertTrue

class GeneratedLearningNoteTypeRequirementsPolicyTest {

    private val policy = GeneratedLearningNoteTypeRequirementsPolicy()

    @Test
    fun `collectIssues returns usage pattern error for phrase without pattern`() {
        val issues = policy.collectIssues(
            sampleWordNote().copy(
                noteType = LearningNoteType.Phrase,
                usagePattern = "",
                cards = listOf(recognitionCard(), productionCard(), clozeCard()),
            )
        )

        assertTrue(issues.any { it.code == IssueCode.MissingUsagePattern })
    }

    @Test
    fun `collectIssues returns missing cloze sentence for sentence pattern`() {
        val issues = policy.collectIssues(
            sampleWordNote().copy(
                noteType = LearningNoteType.SentencePattern,
                usagePattern = "would you mind doing something",
                clozeSentence = "",
                cards = listOf(productionCard()),
            )
        )

        assertTrue(issues.any { it.code == IssueCode.MissingClozeSentence })
        assertTrue(issues.any { it.code == IssueCode.MissingExpectedCardType })
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
            cards = listOf(recognitionCard(), productionCard()),
            qualityChecks = listOf(
                GeneratedNoteQualityCheck(
                    code = GeneratedNoteQualityCode.SingleMeaning,
                    passed = true,
                    message = "ok",
                )
            ),
        )
    }

    private fun recognitionCard(): GeneratedStudyCard {
        return GeneratedStudyCard(
            cardId = "card-rec",
            cardType = StudyCardType.Recognition,
            prompt = "borrow",
            expectedAnswer = "pedir prestado",
            evaluationMode = EvaluationMode.FlexibleText,
        )
    }

    private fun productionCard(): GeneratedStudyCard {
        return GeneratedStudyCard(
            cardId = "card-prod",
            cardType = StudyCardType.Production,
            prompt = "Como dices pedir prestado en ingles?",
            expectedAnswer = "borrow",
            evaluationMode = EvaluationMode.Exact,
        )
    }

    private fun clozeCard(): GeneratedStudyCard {
        return GeneratedStudyCard(
            cardId = "card-cloze",
            cardType = StudyCardType.Cloze,
            prompt = "Can I ____ your pen?",
            expectedAnswer = "borrow",
            evaluationMode = EvaluationMode.Exact,
        )
    }
}
