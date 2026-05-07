package com.emm.domain.generation

import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertTrue

class GeneratedLearningNoteCardsPolicyTest {

    private val policy = GeneratedLearningNoteCardsPolicy()

    @Test
    fun `collectIssues returns no active cards error when all cards inactive`() {
        val result = policy.collectIssues(
            sampleWordNote().copy(
                cards = listOf(
                    recognitionCard().copy(isActive = false),
                    productionCard().copy(isActive = false),
                )
            )
        )

        assertTrue(result.errors.any { it.code == IssueCode.NoActiveCards })
    }

    @Test
    fun `collectIssues returns duplicate active card warning`() {
        val duplicate = productionCard().copy(cardId = "card-prod-2")
        val result = policy.collectIssues(
            sampleWordNote().copy(
                cards = listOf(
                    recognitionCard(),
                    productionCard(),
                    duplicate,
                )
            )
        )

        assertTrue(result.warnings.any { it.code == IssueCode.DuplicateActiveCard })
    }

    @Test
    fun `collectIssues returns prompt equals answer error`() {
        val result = policy.collectIssues(
            sampleWordNote().copy(
                cards = listOf(
                    recognitionCard().copy(prompt = "borrow", expectedAnswer = "borrow"),
                    productionCard(),
                )
            )
        )

        assertTrue(result.errors.any { it.code == IssueCode.CardPromptMatchesAnswer })
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
                recognitionCard(),
                productionCard(),
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
}
