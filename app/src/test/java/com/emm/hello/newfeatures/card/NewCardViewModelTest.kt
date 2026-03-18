package com.emm.hello.newfeatures.card

import app.cash.turbine.test
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardGenerated
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardReadRepository
import com.emm.domain.flashcard.FlashcardWriteRepository
import com.emm.domain.flashcard.GenerateFlashcardPreviewUseCase
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
        val writeRepository = mockk<FlashcardWriteRepository>()
        val readRepository = mockk<FlashcardReadRepository>()
        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1"
        every { defaultDeckSelectionRepository.setDefaultDeckId(any()) } returns Unit
        coEvery { generationRepository.generateFlashcard(any()) } returns sampleGeneratedFlashcard()
        coEvery { writeRepository.create(any()) } returns "card-1"
        coEvery { writeRepository.upsertExamples(any(), any()) } returns Unit
        coEvery { readRepository.fetchById(any()) } returns Flashcard.Empty

        val viewModel = NewCardViewModel(
            getDecksUseCase = GetDecksUseCase(deckRepository),
            createFlashcardUseCase = CreateFlashcardUseCase(writeRepository, readRepository),
            generateFlashcardPreviewUseCase = GenerateFlashcardPreviewUseCase(generationRepository),
            getDefaultDeckUseCase = GetDefaultDeckUseCase(defaultDeckSelectionRepository),
            setDefaultDeckUseCase = SetDefaultDeckUseCase(defaultDeckSelectionRepository),
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onIntent(NewCardUiIntent.WordChanged("updated"))
            val updated = awaitItem()
            assertThat(updated.word).isEqualTo("updated")
            assertThat(updated.error).isNull()
            assertThat(updated.previewResult).isNull()
        }
    }

    @Test
    fun `save clicked success emits show message effect`() = runTest {
        val generationRepository = mockk<FlashcardGenerationRepository>()
        val writeRepository = mockk<FlashcardWriteRepository>()
        val readRepository = mockk<FlashcardReadRepository>()
        val defaultDeckSelectionRepository = mockk<DefaultDeckSelectionRepository>()
        val deckRepository = FakeDeckRepository()

        every { defaultDeckSelectionRepository.getDefaultDeckId() } returns "deck-1"
        every { defaultDeckSelectionRepository.setDefaultDeckId(any()) } returns Unit
        coEvery { generationRepository.generateFlashcard(any()) } returns sampleGeneratedFlashcard()
        coEvery { writeRepository.create(any()) } returns "card-1"
        coEvery { writeRepository.upsertExamples(any(), any()) } returns Unit
        coEvery { readRepository.fetchById(any()) } returns Flashcard.Empty

        val viewModel = NewCardViewModel(
            getDecksUseCase = GetDecksUseCase(deckRepository),
            createFlashcardUseCase = CreateFlashcardUseCase(writeRepository, readRepository),
            generateFlashcardPreviewUseCase = GenerateFlashcardPreviewUseCase(generationRepository),
            getDefaultDeckUseCase = GetDefaultDeckUseCase(defaultDeckSelectionRepository),
            setDefaultDeckUseCase = SetDefaultDeckUseCase(defaultDeckSelectionRepository),
        )

        advanceUntilIdle()
        viewModel.onIntent(NewCardUiIntent.GenerateClicked)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(NewCardUiIntent.SaveClicked)
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(NewCardUiEffect.ShowMessage::class.java)
            assertThat(effect).isEqualTo(NewCardUiEffect.ShowMessage("Tarjeta creada"))
        }

        coVerify(exactly = 1) {
            writeRepository.create(
                match<CreateFlashcardInput> {
                    it.deckId == "deck-1" && it.word == "hello" && it.meaning == "a greeting"
                }
            )
        }
    }

    private class FakeDeckRepository : DeckRepository {
        private val deck = Deck(
            id = "deck-1",
            name = "Main deck",
            description = "",
            createdAt = LocalDateTime.parse("2026-03-16T10:00:00"),
            cards = emptyList(),
            cardsCount = 0L,
        )

        override suspend fun addDeck(deck: CreateDeckInput) = Unit

        override fun findById(deckId: String): Flow<Deck> = flowOf(deck)

        override fun fetchAll(): Flow<List<Deck>> = flowOf(listOf(deck))

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = flowOf(listOf(deck))
    }

    private fun sampleGeneratedFlashcard(): FlashcardGenerated {
        return FlashcardGenerated(
            word = "hello",
            translation = "hola",
            phonetics = "/həˈloʊ/",
            meaning = "a greeting",
            language = "en",
            examples = listOf(
                Example(
                    exampleId = "ex-1",
                    text = "Hello there!",
                    translation = "¡Hola!",
                    type = "greeting",
                )
            ),
            partOfSpeech = "interjection",
            type = "word",
            notes = "",
            tags = listOf("basic"),
        )
    }
}
