package com.emm.hello.newfeatures.card

import app.cash.turbine.test
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.SoftDeleteFlashcardUseCase
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.hello.MainDispatcherRule
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
    fun `init loads flashcard and state reflects id and word`() = runTest {
        val detail = FlashcardDetail(
            flashcard = Flashcard.empty(SystemClock).copy(
                id = "card-1".toFlashcardId(),
                word = "hello",
            ),
        )
        val viewModel = FlashcardDetailViewModel(
            flashcardId = "card-1",
            flashcardRepository = FakeFlashcardReadRepo(detail),
            softDeleteFlashcardUseCase = SoftDeleteFlashcardUseCase(FakeFlashcardReadRepo()),
        )

        assertThat(viewModel.state.value.flashcard.flashcard.id.value).isEqualTo("card-1")
        assertThat(viewModel.state.value.flashcard.flashcard.word).isEqualTo("hello")
    }

    @Test
    fun `load failure emits load failed effect with message`() = runTest {
        val viewModel = FlashcardDetailViewModel(
            flashcardId = "card-1",
            flashcardRepository = FakeFlashcardReadRepo(shouldFail = true),
            softDeleteFlashcardUseCase = SoftDeleteFlashcardUseCase(FakeFlashcardReadRepo()),
        )

        viewModel.effect.test {
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(FlashcardDetailUiEffect.LoadFailed::class.java)
            assertThat((effect as FlashcardDetailUiEffect.LoadFailed).message).isEqualTo("fetch failed")
        }
    }

    private class FakeFlashcardReadRepo(
        private val detail: FlashcardDetail = FlashcardDetail(Flashcard.empty(SystemClock)),
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
        override suspend fun softDeleteFlashcard(flashcardId: FlashcardId) = Unit
        override suspend fun upsertExamples(examples: List<Example>, flashcardId: FlashcardId) = Unit
    }
}