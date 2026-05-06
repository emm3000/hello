package com.emm.domain.authoring

import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardReadRepository
import com.emm.domain.flashcard.FlashcardWriteRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EvaluationMode
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
            writeRepository = FakeWriteRepository(),
            readRepository = FakeReadRepository(),
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
        val writeRepository = FakeWriteRepository()
        val useCase = CreateFlashcardUseCase(
            writeRepository = writeRepository,
            readRepository = FakeReadRepository(),
            validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
            ensureUniqueFlashcardInDeckUseCase = EnsureUniqueFlashcardInDeckUseCase(
                isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                    repository = DuplicateRepoStub(exists = false),
                ),
            ),
        )

        useCase(deckId = "deck-1".toDeckId(), learningNote = sampleWordNote())

        assertEquals(1, writeRepository.createCalls)
        assertEquals(1, writeRepository.upsertExamplesCalls)
    }

    @Test
    fun `invoke rejects exact duplicate in deck`() = runTest {
        val useCase = CreateFlashcardUseCase(
            writeRepository = FakeWriteRepository(),
            readRepository = FakeReadRepository(),
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

private class FakeWriteRepository : FlashcardWriteRepository {
    var createCalls = 0
    var upsertExamplesCalls = 0

    override suspend fun create(input: CreateFlashcardInput): FlashcardId {
        createCalls += 1
        return "flashcard-1".toFlashcardId()
    }

    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) {
        upsertExamplesCalls += 1
    }
}

private class FakeReadRepository : FlashcardReadRepository {
    override fun fetchAll() = throw UnsupportedOperationException()
    override fun fetchByDeckId(deckId: DeckId) = throw UnsupportedOperationException()

    override suspend fun fetchById(id: FlashcardId): Flashcard {
        return Flashcard.empty(SystemClock).copy(id = id)
    }
}

private class DuplicateRepoStub(
    private val exists: Boolean,
) : FlashcardDuplicateRepository {
    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean = exists
}
