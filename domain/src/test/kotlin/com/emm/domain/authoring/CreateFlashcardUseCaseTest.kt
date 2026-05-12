package com.emm.domain.authoring

import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedNoteQualityCode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag
import com.emm.domain.generation.RegisterPreference
import com.emm.domain.generation.StudyCardType
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateFlashcardUseCaseTest {

    @Test
    fun `invoke rejects invalid learning note`() = runTest {
        val useCase = CreateFlashcardUseCase(
            repository = FakeFlashcardRepository(),
            validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
            ensureUniqueFlashcardInDeckUseCase = EnsureUniqueFlashcardInDeckUseCase(
                isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                    repository = DuplicateRepoStub(exists = false),
                ),
            ),
        )

        val error = assertFailsWith<com.emm.domain.validation.DomainValidationException> {
            useCase(
                deckId = "deck-1".toDeckId(),
                learningNote = sampleWordNote().copy(cards = emptyList()),
            )
        }

        assertTrue(error.issues.isNotEmpty())
    }

    @Test
    fun `invoke persists valid learning note`() = runTest {
        val repository = FakeFlashcardRepository()
        val useCase = CreateFlashcardUseCase(
            repository = repository,
            validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
            ensureUniqueFlashcardInDeckUseCase = EnsureUniqueFlashcardInDeckUseCase(
                isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                    repository = DuplicateRepoStub(exists = false),
                ),
            ),
        )

        useCase(deckId = "deck-1".toDeckId(), learningNote = sampleWordNote())

        assertEquals(1, repository.createCalls)
        assertEquals(1, repository.upsertExamplesCalls)
    }

    @Test
    fun `invoke rejects exact duplicate in deck`() = runTest {
        val useCase = CreateFlashcardUseCase(
            repository = FakeFlashcardRepository(),
            validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
            ensureUniqueFlashcardInDeckUseCase = EnsureUniqueFlashcardInDeckUseCase(
                isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                    repository = DuplicateRepoStub(exists = true),
                ),
            ),
        )

        val error = assertFailsWith<com.emm.domain.validation.DomainValidationException> {
            useCase(deckId = "deck-1".toDeckId(), learningNote = sampleWordNote())
        }

        assertTrue(error.issues.any { it.code == com.emm.domain.validation.IssueCode.DuplicateExactCardInDeck })
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

private class FakeFlashcardRepository : FlashcardRepository {
    var createCalls = 0
    var upsertExamplesCalls = 0

    override fun fetchAll() = throw UnsupportedOperationException()
    override fun fetchByDeckId(deckId: DeckId) = throw UnsupportedOperationException()

    override suspend fun create(input: CreateFlashcardInput): FlashcardId {
        createCalls += 1
        return "flashcard-1".toFlashcardId()
    }

    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) {
        upsertExamplesCalls += 1
    }

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        return FlashcardDetail(flashcard = Flashcard.empty(SystemClock).copy(id = id))
    }

    override suspend fun updateFlashcard(input: UpdateFlashcardInput) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId) = Unit
}

private class DuplicateRepoStub(
    private val exists: Boolean,
) : FlashcardDuplicateRepository {
    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean = exists
}
