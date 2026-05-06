package com.emm.domain.authoring

import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.GeneratedNoteQualityCheck
import com.emm.domain.flashcard.GeneratedNoteQualityCode
import com.emm.domain.flashcard.GeneratedStudyCard
import com.emm.domain.flashcard.LearningDomain
import com.emm.domain.flashcard.LearningNoteType
import com.emm.domain.flashcard.LevelBand
import com.emm.domain.flashcard.PartOfSpeechTag
import com.emm.domain.flashcard.RegisterPreference
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.ids.toDeckId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EnsureUniqueFlashcardInDeckUseCaseTest {

    @Test
    fun `invoke throws when duplicate exists`() = runTest {
        val useCase = EnsureUniqueFlashcardInDeckUseCase(
            isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                repository = DuplicateRepo(exists = true),
            )
        )

        assertFailsWith<com.emm.domain.validation.DomainValidationException> {
            useCase(deckId = "deck-1".toDeckId(), note = sampleWordNote())
        }
    }

    @Test
    fun `invoke completes when duplicate does not exist`() = runTest {
        val useCase = EnsureUniqueFlashcardInDeckUseCase(
            isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                repository = DuplicateRepo(exists = false),
            )
        )

        useCase(deckId = "deck-1".toDeckId(), note = sampleWordNote())
    }

    private fun sampleWordNote(): GeneratedLearningNote {
        return GeneratedLearningNote(
            noteId = "note-1",
            noteType = LearningNoteType.Word,
            expression = "borrow",
            intendedMeaningEs = "pedir prestado",
            simpleDefinitionEn = "to take something and return it later",
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
                ),
                GeneratedStudyCard(
                    cardId = "card-2",
                    cardType = StudyCardType.Production,
                    prompt = "Como dices pedir prestado en ingles?",
                    expectedAnswer = "borrow",
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

private class DuplicateRepo(
    private val exists: Boolean,
) : FlashcardDuplicateRepository {
    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean = exists
}
