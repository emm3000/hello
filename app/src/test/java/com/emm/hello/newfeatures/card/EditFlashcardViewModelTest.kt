package com.emm.hello.newfeatures.card

import app.cash.turbine.test
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.SoftDeleteFlashcardUseCase
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.flashcard.UpdateFlashcardUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.hello.MainDispatcherRule
import com.emm.hello.R
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EditFlashcardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init flattens the first example into the fields`() = runTest {
        val viewModel = buildViewModel(FakeFlashcardRepo(detail = detailOf(enrichedCard())))

        val state: EditFlashcardUiState = viewModel.state.value
        assertThat(state.word).isEqualTo("aesthetic")
        assertThat(state.translation).isEqualTo("estético")
        assertThat(state.exampleText).isEqualTo("The building has a strong aesthetic.")
        assertThat(state.exampleTranslation).isEqualTo("El edificio tiene una estética marcada.")
        assertThat(state.partOfSpeech).isEqualTo("adjective")
        assertThat(state.phonetic).isEqualTo("/esˈθetɪk/")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `a blank word invalidates the form`() = runTest {
        val viewModel = buildViewModel(FakeFlashcardRepo(detail = detailOf(enrichedCard())))

        viewModel.onIntent(EditFlashcardUiIntent.WordChanged("   "))

        assertThat(viewModel.state.value.wordError).isEqualTo(R.string.validation_word_required)
        assertThat(viewModel.state.value.isValid).isFalse()
    }

    @Test
    fun `Submit keeps the meaning and the examples tail the form never showed`() = runTest {
        val repo = FakeFlashcardRepo(detail = detailOf(enrichedCard()))
        val viewModel = buildViewModel(repo)

        viewModel.onIntent(EditFlashcardUiIntent.ExampleTextChanged("A new sentence."))
        viewModel.effect.test {
            viewModel.onIntent(EditFlashcardUiIntent.Submit)
            awaitItem()
            awaitItem()
        }

        val input: UpdateFlashcardInput = repo.updated.single()
        assertThat(input.meaning).isEqualTo("pleasing to look at")
        assertThat(input.examples.first().text).isEqualTo("A new sentence.")
        assertThat(input.examples.first().exampleId).isEqualTo("e1")
        assertThat(input.examples.map(Example::exampleId)).containsExactly("e1", "e2").inOrder()
    }

    @Test
    fun `Submit saves an unenriched card whose only content is the word`() = runTest {
        val repo = FakeFlashcardRepo(detail = detailOf(capturedCard()))
        val viewModel = buildViewModel(repo)

        viewModel.onIntent(EditFlashcardUiIntent.TranslationChanged("farol"))
        viewModel.effect.test {
            viewModel.onIntent(EditFlashcardUiIntent.Submit)
            assertThat(awaitItem()).isEqualTo(EditFlashcardUiEffect.ShowMessage(R.string.card_updated_message))
            assertThat(awaitItem()).isEqualTo(EditFlashcardUiEffect.NavigateBack)
        }

        val input: UpdateFlashcardInput = repo.updated.single()
        assertThat(input.word).isEqualTo("lantern")
        assertThat(input.meaning).isEmpty()
        assertThat(input.translation).isEqualTo("farol")
        assertThat(input.examples).isEmpty()
    }

    @Test
    fun `Submit failure resets isSubmitting and reports the error`() = runTest {
        val repo = FakeFlashcardRepo(detail = detailOf(enrichedCard()), updateFails = true)
        val viewModel = buildViewModel(repo)

        viewModel.effect.test {
            viewModel.onIntent(EditFlashcardUiIntent.Submit)
            assertThat(awaitItem()).isEqualTo(EditFlashcardUiEffect.ShowMessage(R.string.error_save_card))
        }

        assertThat(viewModel.state.value.isSubmitting).isFalse()
    }

    @Test
    fun `CloseClicked emits NavigateBack`() = runTest {
        val viewModel = buildViewModel(FakeFlashcardRepo(detail = detailOf(enrichedCard())))

        viewModel.effect.test {
            viewModel.onIntent(EditFlashcardUiIntent.CloseClicked)
            assertThat(awaitItem()).isEqualTo(EditFlashcardUiEffect.NavigateBack)
        }
    }

    @Test
    fun `confirmed delete emits FlashcardDeleted`() = runTest {
        val viewModel = buildViewModel(FakeFlashcardRepo(detail = detailOf(enrichedCard())))

        viewModel.effect.test {
            viewModel.onIntent(EditFlashcardUiIntent.DeleteFlashcard)
            viewModel.onIntent(EditFlashcardUiIntent.ConfirmDeleteFlashcard)
            assertThat(awaitItem()).isEqualTo(EditFlashcardUiEffect.FlashcardDeleted)
        }
    }

    private fun buildViewModel(repo: FakeFlashcardRepo): EditFlashcardViewModel {
        return EditFlashcardViewModel(
            flashcardId = "card-1",
            flashcardRepository = repo,
            updateFlashcardUseCase = UpdateFlashcardUseCase(repo),
            softDeleteFlashcardUseCase = SoftDeleteFlashcardUseCase(repo),
        )
    }

    private fun detailOf(flashcard: Flashcard): FlashcardDetail = FlashcardDetail(flashcard)

    private fun enrichedCard(): Flashcard = Flashcard.empty(SystemClock).copy(
        id = "card-1".toFlashcardId(),
        word = "aesthetic",
        meaning = "pleasing to look at",
        translation = "estético",
        phonetic = "/esˈθetɪk/",
        partOfSpeech = "adjective",
        examples = listOf(
            Example(
                exampleId = "e1",
                text = "The building has a strong aesthetic.",
                translation = "El edificio tiene una estética marcada.",
                type = "usage",
            ),
            Example(
                exampleId = "e2",
                text = "Her Instagram is very aesthetic.",
                translation = "Su Instagram es muy estético.",
                type = "usage",
            ),
        ),
    )

    private fun capturedCard(): Flashcard = Flashcard.empty(SystemClock).copy(
        id = "card-1".toFlashcardId(),
        word = "lantern",
        enrichmentStatus = EnrichmentStatus.FAILED,
    )
}

private class FakeFlashcardRepo(
    private val detail: FlashcardDetail,
    private val updateFails: Boolean = false,
) : FlashcardRepository {

    val updated: MutableList<UpdateFlashcardInput> = mutableListOf()

    override suspend fun fetchById(id: FlashcardId): FlashcardDetail = detail

    override suspend fun update(input: UpdateFlashcardInput) {
        if (updateFails) error("update failed")
        updated += input
    }

    override fun fetchAll(): Flow<List<Flashcard>> = emptyFlow()
    override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = emptyFlow()
    override suspend fun create(input: CreateFlashcardInput): FlashcardId = throw UnsupportedOperationException()
    override suspend fun updateEnrichmentStatus(flashcardId: FlashcardId, status: EnrichmentStatus) = Unit
    override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
    override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
    override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit
    override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
    override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
}
