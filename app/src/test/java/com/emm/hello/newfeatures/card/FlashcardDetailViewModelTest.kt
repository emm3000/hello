package com.emm.hello.newfeatures.card

import app.cash.turbine.test
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.hello.MainDispatcherRule
import com.emm.hello.R
import com.emm.hello.newfeatures.shared.UndoEventHolder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FlashcardDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Load fetches the flashcard and state reflects id and word`() = runTest {
        val viewModel = FlashcardDetailViewModel(
            flashcardId = "card-1",
            flashcardRepository = FakeFlashcardReadRepo(detailOf("hello")),
            undoEventHolder = UndoEventHolder(),
        )

        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        assertThat(viewModel.state.value.flashcard.id.value).isEqualTo("card-1")
        assertThat(viewModel.state.value.flashcard.word).isEqualTo("hello")
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `nothing loads until Load is sent`() = runTest {
        val viewModel = FlashcardDetailViewModel(
            flashcardId = "card-1",
            flashcardRepository = FakeFlashcardReadRepo(detailOf("hello")),
            undoEventHolder = UndoEventHolder(),
        )

        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.flashcard.word).isEmpty()
    }

    @Test
    fun `a second Load reflects the card as it is now`() = runTest {
        val repo = FakeFlashcardReadRepo(detailOf("hello"))
        val viewModel = FlashcardDetailViewModel(
            flashcardId = "card-1",
            flashcardRepository = repo,
            undoEventHolder = UndoEventHolder(),
        )
        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        repo.detail = detailOf("hello there")
        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        assertThat(viewModel.state.value.flashcard.word).isEqualTo("hello there")
    }

    @Test
    fun `BackClicked emits NavigateBack`() = runTest {
        val viewModel = FlashcardDetailViewModel(
            flashcardId = "card-1",
            flashcardRepository = FakeFlashcardReadRepo(),
            undoEventHolder = UndoEventHolder(),
        )

        viewModel.effect.test {
            viewModel.onIntent(FlashcardDetailUiIntent.BackClicked)
            assertThat(awaitItem()).isEqualTo(FlashcardDetailUiEffect.NavigateBack)
        }
    }

    @Test
    fun `load failure emits load failed effect with message`() = runTest {
        val viewModel = FlashcardDetailViewModel(
            flashcardId = "card-1",
            flashcardRepository = FakeFlashcardReadRepo(shouldFail = true),
            undoEventHolder = UndoEventHolder(),
        )

        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        viewModel.effect.test {
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(FlashcardDetailUiEffect.LoadFailed::class.java)
            assertThat((effect as FlashcardDetailUiEffect.LoadFailed).messageRes)
                .isEqualTo(R.string.error_load_card)
        }
    }

    private fun detailOf(word: String): FlashcardDetail = FlashcardDetail(
        flashcard = Flashcard.empty(SystemClock).copy(
            id = "card-1".toFlashcardId(),
            word = word,
        ),
    )

    private class FakeFlashcardReadRepo(
        var detail: FlashcardDetail = FlashcardDetail(Flashcard.empty(SystemClock)),
        private val shouldFail: Boolean = false,
    ) : FlashcardRepository {
        override fun fetchAll(): Flow<List<Flashcard>> = emptyFlow()
        override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = emptyFlow()
        override suspend fun fetchById(id: FlashcardId): FlashcardDetail {
            if (shouldFail) error("fetch failed")
            return detail
        }
        override suspend fun create(input: CreateFlashcardInput): FlashcardId = throw UnsupportedOperationException()
        override suspend fun update(input: UpdateFlashcardInput) = throw UnsupportedOperationException()
        override suspend fun updateEnrichmentStatus(
            flashcardId: FlashcardId,
            status: EnrichmentStatus,
            failure: EnrichmentFailure?,
        ) = Unit
        override suspend fun recordPromptVersion(flashcardId: FlashcardId, promptVersion: Int) = Unit
        override suspend fun softDeleteFlashcard(flashcardId: FlashcardId): Long = 0L
        override suspend fun restoreFlashcard(flashcardId: FlashcardId, deletedAt: Long) = Unit
        override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit
        override suspend fun countDueFlashcards(nowMillis: Long): Long = 0L
        override suspend fun fetchRecentWords(limit: Int): List<String> = emptyList()
    }
}
