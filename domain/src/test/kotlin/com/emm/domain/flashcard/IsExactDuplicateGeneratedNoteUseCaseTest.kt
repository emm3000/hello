package com.emm.domain.flashcard

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsExactDuplicateGeneratedNoteUseCaseTest {

    @Test
    fun `invoke returns true when repository has exact duplicate`() = runTest {
        val repository = DuplicateRepoSpy(exists = true)
        val useCase = IsExactDuplicateGeneratedNoteUseCase(repository)

        val result = useCase(deckId = " deck-1 ", note = sampleWordNote())

        assertTrue(result)
        assertEquals("deck-1|run|correr|word", repository.lastKey?.canonicalValue)
    }

    @Test
    fun `invoke returns false when repository does not have exact duplicate`() = runTest {
        val repository = DuplicateRepoSpy(exists = false)
        val useCase = IsExactDuplicateGeneratedNoteUseCase(repository)

        val result = useCase(deckId = "deck-1", note = sampleWordNote())

        assertFalse(result)
    }

    private fun sampleWordNote(): GeneratedLearningNote {
        return GeneratedLearningNote(
            noteId = "note-1",
            noteType = LearningNoteType.Word,
            expression = "  RUN  ",
            intendedMeaningEs = "  correr ",
            simpleDefinitionEn = "to move fast",
            partOfSpeech = PartOfSpeechTag.Verb,
            register = RegisterPreference.Neutral,
            levelBand = LevelBand.A1_A2,
            domain = LearningDomain.DailyLife,
            whyUseful = "Sirve para hablar de actividad fisica.",
            exampleSentence = "I run every morning.",
            exampleTranslation = "Corro todas las mananas.",
            cards = listOf(
                GeneratedStudyCard(
                    cardId = "card-rec",
                    cardType = StudyCardType.Recognition,
                    prompt = "run",
                    expectedAnswer = "correr",
                    evaluationMode = EvaluationMode.FlexibleText,
                ),
                GeneratedStudyCard(
                    cardId = "card-prod",
                    cardType = StudyCardType.Production,
                    prompt = "Como se dice correr en ingles?",
                    expectedAnswer = "run",
                    evaluationMode = EvaluationMode.Exact,
                ),
            ),
            qualityChecks = listOf(
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
            ),
        )
    }
}

private class DuplicateRepoSpy(
    private val exists: Boolean,
) : FlashcardDuplicateRepository {
    var lastKey: ExactDuplicateKey? = null

    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean {
        lastKey = key
        return exists
    }
}
