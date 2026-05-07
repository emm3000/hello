package com.emm.domain.generation

import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.validation.IssueCode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidateGeneratedLearningNoteUseCaseTest {

    private val useCase = ValidateGeneratedLearningNoteUseCase()

    @Test
    fun `invoke with valid word note returns valid`() {
        val result = useCase(sampleWordNote())

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `invoke with phrase note without usage pattern returns error`() {
        val result = useCase(
            sampleWordNote().copy(
                noteType = LearningNoteType.Phrase,
                cards = listOf(
                    recognitionCard(),
                    productionCard(),
                    clozeCard(),
                ),
                usagePattern = "",
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.MissingUsagePattern })
    }

    @Test
    fun `invoke with missing single meaning quality check returns error`() {
        val result = useCase(
            sampleWordNote().copy(
                qualityChecks = listOf(
                    GeneratedNoteQualityCheck(
                        code = GeneratedNoteQualityCode.NaturalExample,
                        passed = true,
                        message = "ok",
                    )
                )
            )
        )

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.code == IssueCode.MissingSingleMeaningQualityCheck
            }
        )
    }

    @Test
    fun `invoke with sentence pattern without cloze returns error`() {
        val result = useCase(
            sampleWordNote().copy(
                noteType = LearningNoteType.SentencePattern,
                usagePattern = "would you mind doing something",
                clozeSentence = "",
                cards = listOf(
                    productionCard(),
                ),
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.MissingClozeSentence })
        assertTrue(result.errors.any { it.code == IssueCode.MissingExpectedCardType })
    }

    @Test
    fun `invoke with empty card prompt returns error`() {
        val result = useCase(
            sampleWordNote().copy(
                cards = listOf(
                    recognitionCard().copy(prompt = ""),
                    productionCard(),
                )
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.EmptyCardPrompt })
    }

    @Test
    fun `invoke with all cards inactive returns error`() {
        val result = useCase(
            sampleWordNote().copy(
                cards = listOf(
                    recognitionCard().copy(isActive = false),
                    productionCard().copy(isActive = false),
                )
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.NoActiveCards })
    }

    @Test
    fun `invoke with prompt equal to answer returns error`() {
        val result = useCase(
            sampleWordNote().copy(
                cards = listOf(
                    recognitionCard().copy(
                        prompt = "borrow",
                        expectedAnswer = "borrow",
                    ),
                    productionCard(),
                )
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.CardPromptMatchesAnswer })
    }

    @Test
    fun `invoke with duplicate active cards returns warning`() {
        val duplicate = productionCard().copy(cardId = "card-prod-2")
        val result = useCase(
            sampleWordNote().copy(
                cards = listOf(
                    recognitionCard(),
                    productionCard(),
                    duplicate,
                )
            )
        )

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == IssueCode.DuplicateActiveCard })
    }

    @Test
    fun `invoke with failed quality check returns error`() {
        val result = useCase(
            sampleWordNote().copy(
                qualityChecks = fullQualityChecks().map { check ->
                    if (check.code == GeneratedNoteQualityCode.NonAmbiguousAnswers) {
                        check.copy(passed = false, message = "La respuesta sigue siendo ambigua.")
                    } else {
                        check
                    }
                }
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == IssueCode.FailedQualityCheck })
    }

    private fun sampleWordNote(): GeneratedLearningNote {
        return GeneratedLearningNote(
            noteId = "note-1",
            noteType = LearningNoteType.Word,
            expression = "borrow".toExpression(),
            intendedMeaningEs = "pedir prestado".toIntendedMeaningEs(),
            simpleDefinitionEn = "to take something from someone and return it later".toDefinitionEn(),
            partOfSpeech = PartOfSpeechTag.Verb,
            register = RegisterPreference.Neutral,
            levelBand = LevelBand.A1_A2,
            domain = LearningDomain.DailyLife,
            whyUseful = "Sirve para hablar de prestamos en situaciones cotidianas.",
            exampleSentence = "Can I borrow your pen for a minute?",
            exampleTranslation = "Puedo pedirte prestado tu lapicero por un minuto?",
            cards = listOf(
                recognitionCard(),
                productionCard(),
            ),
            qualityChecks = fullQualityChecks(),
            collocations = listOf("borrow money", "borrow a book"),
        )
    }

    private fun fullQualityChecks(): List<GeneratedNoteQualityCheck> {
        return listOf(
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.SingleMeaning,
                passed = true,
                message = "La nota trabaja un solo significado.",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.NaturalExample,
                passed = true,
                message = "El ejemplo suena natural.",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.ExampleSupportsMeaning,
                passed = true,
                message = "El ejemplo sostiene bien el significado.",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.NonAmbiguousAnswers,
                passed = true,
                message = "Las respuestas esperadas son claras.",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.RequiredFieldsPresent,
                passed = true,
                message = "Los campos clave estan completos.",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.ClearCardFocus,
                passed = true,
                message = "Cada card tiene un foco claro.",
            ),
            GeneratedNoteQualityCheck(
                code = GeneratedNoteQualityCode.NoteCardAlignment,
                passed = true,
                message = "Las cards estan alineadas con la nota.",
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
            prompt = "Como dices 'pedir prestado' en ingles?",
            expectedAnswer = "borrow",
            evaluationMode = EvaluationMode.Exact,
        )
    }

    private fun clozeCard(): GeneratedStudyCard {
        return GeneratedStudyCard(
            cardId = "card-cloze",
            cardType = StudyCardType.Cloze,
            prompt = "Can I ____ your pen for a minute?",
            expectedAnswer = "borrow",
            evaluationMode = EvaluationMode.Exact,
        )
    }
}
