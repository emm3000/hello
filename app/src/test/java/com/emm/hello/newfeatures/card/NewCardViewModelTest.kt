package com.emm.hello.newfeatures.card

import app.cash.turbine.test
import com.emm.domain.authoring.CreateFlashcardUseCase
import com.emm.domain.authoring.EnsureUniqueFlashcardInDeckUseCase
import com.emm.domain.authoring.GeneratedLearningNoteMapper
import com.emm.domain.authoring.IsExactDuplicateGeneratedNoteUseCase
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DeckSearchCriteria
import com.emm.domain.deck.Tag
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardInputType
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.GenerateLearningNotePreviewUseCase
import com.emm.domain.generation.GeneratedExampleDraft
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedNoteQualityCode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag
import com.emm.domain.generation.RegenerableNoteField
import com.emm.domain.flashcard.RegenerateLearningNoteClozeUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteExampleUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteFieldUseCase
import com.emm.domain.flashcard.RegenerateStudyCardUseCase
import com.emm.domain.generation.RegisterPreference
import com.emm.domain.generation.StudyCardType
import com.emm.domain.flashcard.ValidateFlashcardGenerationInputUseCase
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toDeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class NewCardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `word changed clears error and preview`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(any()) } returns sampleGeneratedLearningNote()
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.WordChanged("hello"))
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.state.test {
            skipItems(1)
            viewModel.onIntent(NewCardUiIntent.WordChanged("updated"))
            val updated = awaitItem()
            assertThat(updated.word).isEqualTo("updated")
            assertThat(updated.error).isNull()
            assertThat(updated.learningNotePreview).isNull()
            assertThat(updated.canSavePreview).isFalse()
        }
    }

    @Test
    fun `save clicked success emits show message effect`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(any()) } returns sampleGeneratedLearningNote()
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.WordChanged("hello"))
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.effect.test {
            assertThat(awaitItem()).isEqualTo(NewCardUiEffect.OpenReview)
            viewModel.onIntent(NewCardUiIntent.SaveClicked)
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(NewCardUiEffect.ShowMessage::class.java)
            assertThat(effect).isEqualTo(NewCardUiEffect.ShowMessage("Tarjeta creada"))
            assertThat(awaitItem()).isEqualTo(NewCardUiEffect.CloseFlow)
        }

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.canSavePreview).isFalse()

        coVerify(exactly = 1) {
            flashcardRepository.create(
                match<CreateFlashcardInput> {
                    it.deckId.value == "deck-1" && it.word == "hello" && it.meaning == "a greeting"
                }
            )
        }
    }

    @Test
    fun `generate clicked with valid note enables save preview`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(any()) } returns sampleGeneratedLearningNote()
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.WordChanged("hello"))
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        assertThat(viewModel.state.value.learningNotePreview).isNotNull()
        assertThat(viewModel.state.value.canSavePreview).isTrue()
        assertThat(viewModel.state.value.previewValidationIssues).isEmpty()
    }

    @Test
    fun `generate clicked with ai help builds communicative goal input`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()
        val inputSlot = slot<FlashcardGenerationInput>()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(capture(inputSlot)) } returns sampleGeneratedLearningNote()
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.TypeViewSelected(TypeView.WithAiHelp))
        viewModel.onIntent(
            NewCardUiIntent.AiRequestChanged(
                "Quiero aprender frases para pedir comida en un restaurante",
            )
        )
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        assertThat(inputSlot.isCaptured).isTrue()
        assertThat(inputSlot.captured.inputType).isEqualTo(FlashcardInputType.CommunicativeGoal)
        assertThat(inputSlot.captured.userText)
            .isEqualTo("Quiero aprender frases para pedir comida en un restaurante")
        assertThat(inputSlot.captured.communicativeIntentId).isEqualTo("order_food")
        assertThat(inputSlot.captured.domain).isEqualTo(LearningDomain.DailyLife)
        assertThat(viewModel.state.value.canSavePreview).isTrue()
    }

    @Test
    fun `editing required preview field revalidates and can disable save`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(any()) } returns sampleGeneratedLearningNote()
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.WordChanged("hello"))
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.onIntent(
            NewCardUiIntent.PreviewFieldChanged(
                field = EditableLearningNoteField.ExampleSentence,
                value = "",
            )
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.learningNotePreview?.exampleSentence).isEmpty()
        assertThat(viewModel.state.value.canSavePreview).isFalse()
        assertThat(viewModel.state.value.previewValidationIssues).isNotEmpty()
    }

    @Test
    fun `editing preview card updates derived card in preview`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(any()) } returns sampleGeneratedLearningNote()
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.WordChanged("hello"))
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.onIntent(
            NewCardUiIntent.PreviewCardExpectedAnswerChanged(
                cardId = "card-2",
                expectedAnswer = "hi",
            )
        )
        advanceUntilIdle()

        assertThat(
            viewModel.state.value.learningNotePreview
                ?.cards
                ?.first { it.cardId == "card-2" }
                ?.expectedAnswer
        ).isEqualTo("hi")
    }

    @Test
    fun `editing preview card hint and active state updates derived card in preview`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(any()) } returns sampleGeneratedLearningNote()
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.WordChanged("hello"))
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.onIntent(
            NewCardUiIntent.PreviewCardHintChanged(
                cardId = "card-2",
                hint = "Usala para saludar al empezar una conversacion.",
            )
        )
        viewModel.onIntent(
            NewCardUiIntent.PreviewCardActiveChanged(
                cardId = "card-2",
                isActive = false,
            )
        )
        advanceUntilIdle()

        val updatedCard = viewModel.state.value.learningNotePreview
            ?.cards
            ?.first { it.cardId == "card-2" }

        assertThat(updatedCard?.hint).isEqualTo("Usala para saludar al empezar una conversacion.")
        assertThat(updatedCard?.isActive).isFalse()
    }

    @Test
    fun `regenerate example updates preview fields`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(any()) } returns sampleGeneratedLearningNote()
        coEvery { generationRepository.regenerateExample(any(), any()) } returns GeneratedExampleDraft(
            sentence = "Hello, how have you been lately?",
            translation = "Hola, como has estado ultimamente?",
        )
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.WordChanged("hello"))
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.onIntent(NewCardUiIntent.RegenerateExampleClicked)
        advanceUntilIdle()

        assertThat(viewModel.state.value.learningNotePreview?.exampleSentence)
            .isEqualTo("Hello, how have you been lately?")
        assertThat(viewModel.state.value.learningNotePreview?.exampleTranslation)
            .isEqualTo("Hola, como has estado ultimamente?")
    }

    @Test
    fun `regenerate field updates note value`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val flashcardRepository = mockk<FlashcardRepository>()

        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1".toDeckId()
        coEvery { generationRepository.generateLearningNote(any()) } returns sampleGeneratedLearningNote()
        coEvery {
            generationRepository.regenerateNoteField(any(), any(), RegenerableNoteField.WhyUseful)
        } returns "Te ayuda a sonar natural al saludar."
        stubDefaultRepositories(defaultDeckSelectionRepository, flashcardRepository)

        val viewModel = buildViewModel(
            generationRepository = generationRepository,
            flashcardRepository = flashcardRepository,

            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
            deckRepository = deckRepository,
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.WordChanged("hello"))
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.onIntent(NewCardUiIntent.RegenerateFieldClicked(EditableLearningNoteField.WhyUseful))
        advanceUntilIdle()

        assertThat(viewModel.state.value.learningNotePreview?.whyUseful)
            .isEqualTo("Te ayuda a sonar natural al saludar.")
    }

    private class FakeDeckRepository : DeckRepository {
        private val deck = Deck(
            id = "deck-1".toDeckId(),
            name = "Main deck",
            description = "",
            createdAt = LocalDateTime.parse("2026-03-16T10:00:00"),
            cards = emptyList(),
            cardsCount = 0L,
        )

        override suspend fun create(deck: CreateDeckInput) = Unit

        override fun fetchById(deckId: DeckId): Flow<Deck> = flowOf(deck)

        override fun fetchAll(): Flow<List<Deck>> = flowOf(listOf(deck))

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = flowOf(listOf(deck))

        override fun observeFiltered(criteria: DeckSearchCriteria): Flow<List<Deck>> = flowOf(listOf(deck))

        override fun fetchTagsForDeck(deckId: DeckId): Flow<List<Tag>> = emptyFlow()

        override suspend fun update(input: UpdateDeckInput) = Unit
        override suspend fun softDeleteDeck(deckId: DeckId) = Unit
    }

    private fun buildViewModel(
        generationRepository: FlashcardGenerationRepository,
        flashcardRepository: FlashcardRepository,
        defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
        deckRepository: DeckRepository,
    ): NewCardViewModel {
        val validateInputUseCase = ValidateFlashcardGenerationInputUseCase()
        val validateGeneratedLearningNoteUseCase = ValidateGeneratedLearningNoteUseCase()
        return NewCardViewModel(
            getDecksUseCase = GetDecksUseCase(deckRepository),
            generationDependencies = NewCardGenerationDependencies(
                createFlashcardUseCase = CreateFlashcardUseCase(
                    flashcardRepository,
                    validateGeneratedLearningNoteUseCase,
                    EnsureUniqueFlashcardInDeckUseCase(
                        IsExactDuplicateGeneratedNoteUseCase(FakeDuplicateRepository())
                    ),
                    GeneratedLearningNoteMapper(
                        object : com.emm.domain.authoring.LearningNoteArtifactSerializer {
                            override fun encode(
                                studyCards: List<com.emm.domain.generation.GeneratedStudyCard>,
                                qualityChecks: List<com.emm.domain.generation.GeneratedNoteQualityCheck>,
                            ) = com.emm.domain.authoring.EncodedLearningArtifacts("[]", "[]")
                        }
                    ),
                ),
                generateLearningNotePreviewUseCase = GenerateLearningNotePreviewUseCase(
                    repository = generationRepository,
                    validateInputUseCase = validateInputUseCase,
                    validateGeneratedLearningNoteUseCase = validateGeneratedLearningNoteUseCase,
                ),
                regenerateLearningNoteExampleUseCase = RegenerateLearningNoteExampleUseCase(
                    generationRepository,
                    validateInputUseCase,
                ),
                regenerateLearningNoteClozeUseCase = RegenerateLearningNoteClozeUseCase(
                    generationRepository,
                    validateInputUseCase,
                ),
                regenerateLearningNoteFieldUseCase = RegenerateLearningNoteFieldUseCase(
                    generationRepository,
                    validateInputUseCase,
                ),
                regenerateStudyCardUseCase = RegenerateStudyCardUseCase(
                    generationRepository,
                    validateInputUseCase,
                ),
                validateInputUseCase = validateInputUseCase,
                validateGeneratedLearningNoteUseCase = validateGeneratedLearningNoteUseCase,
            ),
            defaultDeckSelectionRepository = defaultDeckSelectionRepository,
        )
    }

    private fun stubDefaultRepositories(
        defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
        flashcardRepository: FlashcardRepository,
    ) {
        val createdFlashcard = Flashcard.empty(SystemClock).copy(id = "card-1".toFlashcardId())
        every { defaultDeckSelectionRepository.setDefaultDeckId(any()) } returns Unit
        coEvery { flashcardRepository.create(any()) } returns "card-1".toFlashcardId()
        coEvery { flashcardRepository.upsertExamples(any(), any()) } returns Unit
        coEvery { flashcardRepository.fetchById(any()) } returns FlashcardDetail(createdFlashcard)
    }

    private fun sampleGeneratedLearningNote(): GeneratedLearningNote {
        return GeneratedLearningNote(
            noteId = "note-1",
            noteType = LearningNoteType.Word,
            expression = "hello".toExpression(),
            intendedMeaningEs = "hola".toIntendedMeaningEs(),
            simpleDefinitionEn = "a greeting".toDefinitionEn(),
            partOfSpeech = PartOfSpeechTag.Interjection,
            register = RegisterPreference.Neutral,
            levelBand = LevelBand.A1_A2,
            domain = LearningDomain.DailyLife,
            whyUseful = "Sirve para saludar en situaciones cotidianas.",
            exampleSentence = "Hello there!",
            exampleTranslation = "¡Hola!",
            cards = listOf(
                GeneratedStudyCard(
                    cardId = "card-1",
                    cardType = StudyCardType.Recognition,
                    prompt = "hello",
                    expectedAnswer = "hola",
                    evaluationMode = EvaluationMode.FlexibleText,
                ),
                GeneratedStudyCard(
                    cardId = "card-2",
                    cardType = StudyCardType.Production,
                    prompt = "Como dices hola en ingles?",
                    expectedAnswer = "hello",
                    evaluationMode = EvaluationMode.Exact,
                )
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

private class FakeDuplicateRepository : FlashcardDuplicateRepository {
    override suspend fun existsExactDuplicate(key: com.emm.domain.flashcard.ExactDuplicateKey): Boolean = false
}
