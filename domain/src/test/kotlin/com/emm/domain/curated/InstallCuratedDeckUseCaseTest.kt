package com.emm.domain.curated

import com.emm.domain.authoring.CreateFlashcardUseCase
import com.emm.domain.authoring.EnsureUniqueFlashcardInDeckUseCase
import com.emm.domain.authoring.GeneratedLearningNoteMapper
import com.emm.domain.authoring.IsExactDuplicateGeneratedNoteUseCase
import com.emm.domain.authoring.sampleWordNote
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.DeckRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.ExactDuplicateKey
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Expression
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.StudyCardType
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.domain.validation.DomainValidationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InstallCuratedDeckUseCaseTest {

    @Test
    fun `installing creates the deck and every note`() = runTest {
        val deckRepository = FakeDeckRepository()
        val flashcardRepository = FakeFlashcardRepository()
        val useCase: InstallCuratedDeckUseCase = installUseCase(
            deckRepository = deckRepository,
            flashcardRepository = flashcardRepository,
        )

        val deckId: DeckId = useCase(CURATED_DECK_ID)

        assertEquals(DeckId.from("curated-test-deck"), deckId)
        val created: CreateDeckInput = assertNotNull(deckRepository.created[deckId])
        assertEquals("Test deck", created.name)
        assertEquals("A curated deck for tests.", created.description)
        assertEquals(listOf("test"), created.tags)
        assertEquals(2, flashcardRepository.cardsIn(deckId).size)
    }

    @Test
    fun `installing twice adds nothing`() = runTest {
        val deckRepository = FakeDeckRepository()
        val flashcardRepository = FakeFlashcardRepository()
        val useCase: InstallCuratedDeckUseCase = installUseCase(
            deckRepository = deckRepository,
            flashcardRepository = flashcardRepository,
        )

        val firstDeckId: DeckId = useCase(CURATED_DECK_ID)
        val secondDeckId: DeckId = useCase(CURATED_DECK_ID)

        assertEquals(firstDeckId, secondDeckId)
        assertEquals(1, deckRepository.createCalls)
        assertEquals(2, flashcardRepository.cardsIn(firstDeckId).size)
    }

    @Test
    fun `installing completes a partially installed deck`() = runTest {
        val deckRepository = FakeDeckRepository()
        val flashcardRepository = FakeFlashcardRepository()
        val deckId: DeckId = DeckId.from("curated-test-deck")
        deckRepository.create(
            CreateDeckInput(
                name = "Test deck",
                description = "A curated deck for tests.",
                tags = listOf("test"),
                id = deckId,
            ),
        )
        createFlashcardUseCase(flashcardRepository)(deckId = deckId, learningNote = firstNote())

        val useCase: InstallCuratedDeckUseCase = installUseCase(
            deckRepository = deckRepository,
            flashcardRepository = flashcardRepository,
        )
        useCase(CURATED_DECK_ID)

        assertEquals(1, deckRepository.createCalls)
        assertEquals(2, flashcardRepository.cardsIn(deckId).size)
    }

    @Test
    fun `an unknown curated id fails`() = runTest {
        val deckRepository = FakeDeckRepository()
        val flashcardRepository = FakeFlashcardRepository()
        val useCase: InstallCuratedDeckUseCase = installUseCase(
            deckRepository = deckRepository,
            flashcardRepository = flashcardRepository,
        )

        assertFailsWith<UnknownCuratedDeckException> { useCase("missing-deck") }

        assertTrue(deckRepository.created.isEmpty())
        assertTrue(flashcardRepository.created.isEmpty())
    }

    @Test
    fun `a validation failure that is not a duplicate propagates`() = runTest {
        val deckRepository = FakeDeckRepository()
        val flashcardRepository = FakeFlashcardRepository()
        val useCase = InstallCuratedDeckUseCase(
            catalog = FakeCuratedDeckCatalog(listOf(curatedDeck(listOf(firstNote().copy(whyUseful = " "))))),
            deckRepository = deckRepository,
            createFlashcardUseCase = createFlashcardUseCase(flashcardRepository),
        )

        assertFailsWith<DomainValidationException> { useCase(CURATED_DECK_ID) }

        assertTrue(flashcardRepository.created.isEmpty())
    }
}

private const val CURATED_DECK_ID = "test-deck"

private fun installUseCase(
    deckRepository: DeckRepository,
    flashcardRepository: FakeFlashcardRepository,
): InstallCuratedDeckUseCase {
    return InstallCuratedDeckUseCase(
        catalog = FakeCuratedDeckCatalog(listOf(curatedDeck(listOf(firstNote(), secondNote())))),
        deckRepository = deckRepository,
        createFlashcardUseCase = createFlashcardUseCase(flashcardRepository),
    )
}

private fun createFlashcardUseCase(flashcardRepository: FakeFlashcardRepository): CreateFlashcardUseCase {
    return CreateFlashcardUseCase(
        repository = flashcardRepository,
        validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase(),
        ensureUniqueFlashcardInDeckUseCase = EnsureUniqueFlashcardInDeckUseCase(
            isExactDuplicateGeneratedNoteUseCase = IsExactDuplicateGeneratedNoteUseCase(
                repository = flashcardRepository,
            ),
        ),
        generatedLearningNoteMapper = GeneratedLearningNoteMapper(),
    )
}

private fun curatedDeck(notes: List<GeneratedLearningNote>): CuratedDeck {
    return CuratedDeck(
        id = CURATED_DECK_ID,
        name = "Test deck",
        description = "A curated deck for tests.",
        tags = listOf("test"),
        levelBand = LevelBand.A1_A2,
        notes = notes,
    )
}

private fun firstNote(): GeneratedLearningNote = sampleWordNote()

private fun secondNote(): GeneratedLearningNote {
    return sampleWordNote().copy(
        noteId = "note-2",
        expression = "lend".toExpression(),
        intendedMeaningEs = "prestar".toIntendedMeaningEs(),
        exampleSentence = "Can you lend me your pen?",
        exampleTranslation = "Puedes prestarme tu lapicero?",
        cards = listOf(
            GeneratedStudyCard(
                cardId = "card-3",
                cardType = StudyCardType.Recognition,
                prompt = "lend",
                expectedAnswer = "prestar",
                evaluationMode = EvaluationMode.FlexibleText,
            ),
            GeneratedStudyCard(
                cardId = "card-4",
                cardType = StudyCardType.Production,
                prompt = "Como dices prestar en ingles?",
                expectedAnswer = "lend",
                evaluationMode = EvaluationMode.Exact,
            ),
        ),
    )
}

private class FakeFlashcardRepository : FlashcardRepository, FlashcardDuplicateRepository {

    val created: MutableList<CreateFlashcardInput> = mutableListOf()

    fun cardsIn(deckId: DeckId): List<CreateFlashcardInput> = created.filter { it.deckId == deckId }

    override suspend fun create(input: CreateFlashcardInput): FlashcardId {
        created += input
        return "flashcard-${created.size}".toFlashcardId()
    }

    override suspend fun existsExactDuplicate(key: ExactDuplicateKey): Boolean {
        return created.any { it.duplicateKey().canonicalValue == key.canonicalValue }
    }

    override suspend fun existsExpressionInDeck(deckId: DeckId, expression: Expression): Boolean = false

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
        return FlashcardDetail(flashcard = Flashcard.empty(SystemClock).copy(id = id))
    }

    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit

    override fun fetchAll(): Flow<List<Flashcard>> = throw UnsupportedOperationException()
    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = throw UnsupportedOperationException()
    override suspend fun update(input: UpdateFlashcardInput) = Unit
    override suspend fun updateEnrichmentStatus(
        flashcardId: FlashcardId,
        status: EnrichmentStatus,
        failureReason: String?,
    ) = Unit
    override suspend fun recordPromptVersion(flashcardId: FlashcardId, promptVersion: Int) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
}

private fun CreateFlashcardInput.duplicateKey(): ExactDuplicateKey {
    return ExactDuplicateKey.from(
        deckId = deckId,
        expression = word.toExpression(),
        intendedMeaningEs = translation.toIntendedMeaningEs(),
        noteType = LearningNoteType.valueOf(noteType),
    )
}
