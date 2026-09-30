package com.emm.hello.newfeatures.card

import app.cash.turbine.test
import com.emm.domain.authoring.RetryEnrichmentUseCase
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.UpdateFlashcardInput
import com.emm.domain.generation.EnrichmentFailure
import com.emm.domain.generation.EnrichmentFailureCause
import com.emm.domain.generation.GenerationCredits
import com.emm.domain.generation.GenerationCreditsRepository
import com.emm.domain.generation.InputProblem
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.Clock
import com.emm.domain.time.SystemClock
import com.emm.hello.MainDispatcherRule
import com.emm.hello.R
import com.emm.hello.newfeatures.shared.UndoEventHolder
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FlashcardDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Load fetches the flashcard and state reflects id and word`() = runTest {
        val viewModel: FlashcardDetailViewModel = buildViewModel(FakeFlashcardReadRepo(detailOf("hello")))

        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        assertThat(viewModel.state.value.flashcard.id.value).isEqualTo("card-1")
        assertThat(viewModel.state.value.flashcard.word).isEqualTo("hello")
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun `nothing loads until Load is sent`() = runTest {
        val viewModel: FlashcardDetailViewModel = buildViewModel(FakeFlashcardReadRepo(detailOf("hello")))

        assertThat(viewModel.state.value.isLoading).isTrue()
        assertThat(viewModel.state.value.flashcard.word).isEmpty()
    }

    @Test
    fun `the card reflects changes to it while it is open`() = runTest {
        val repo = FakeFlashcardReadRepo(detailOf("hello"))
        val viewModel: FlashcardDetailViewModel = buildViewModel(repo)
        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        repo.detail.value = detailOf("hello there")

        assertThat(viewModel.state.value.flashcard.word).isEqualTo("hello there")
    }

    @Test
    fun `BackClicked emits NavigateBack`() = runTest {
        val viewModel: FlashcardDetailViewModel = buildViewModel(FakeFlashcardReadRepo())

        viewModel.effect.test {
            viewModel.onIntent(FlashcardDetailUiIntent.BackClicked)
            assertThat(awaitItem()).isEqualTo(FlashcardDetailUiEffect.NavigateBack)
        }
    }

    @Test
    fun `load failure emits load failed effect with message`() = runTest {
        val viewModel: FlashcardDetailViewModel = buildViewModel(FakeFlashcardReadRepo(shouldFail = true))

        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        viewModel.effect.test {
            val effect = awaitItem()
            assertThat(effect).isInstanceOf(FlashcardDetailUiEffect.LoadFailed::class.java)
            assertThat((effect as FlashcardDetailUiEffect.LoadFailed).messageRes)
                .isEqualTo(R.string.error_load_card)
        }
    }

    @Test
    fun `a card that has not failed offers no failure action`() = runTest {
        val viewModel: FlashcardDetailViewModel = buildViewModel(FakeFlashcardReadRepo(detailOf("hello")))

        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        assertThat(viewModel.state.value.failedEnrichment).isNull()
    }

    @Test
    fun `each failure cause offers its own action`() = runTest {
        val expected: Map<EnrichmentFailureCause, FailedEnrichmentAction> = mapOf(
            EnrichmentFailureCause.Technical to FailedEnrichmentAction.TryAgain,
            EnrichmentFailureCause.AppCheckRejected to FailedEnrichmentAction.WriteItMyself,
            EnrichmentFailureCause.WordProblem(InputProblem.Contradictory) to FailedEnrichmentAction.WriteItMyself,
        )

        expected.forEach { (cause, action) ->
            val viewModel: FlashcardDetailViewModel = buildViewModel(FakeFlashcardReadRepo(failedDetail(cause)))
            viewModel.onIntent(FlashcardDetailUiIntent.Load)

            assertThat(viewModel.state.value.failedEnrichment?.action).isEqualTo(action)
        }
    }

    @Test
    fun `exhausted credits offer no action before the reset and keep the reset time`() = runTest {
        val credits = GenerationCredits(remaining = 0, resetAt = NOW.plusSeconds(3_600))
        val viewModel: FlashcardDetailViewModel = buildViewModel(
            FakeFlashcardReadRepo(failedDetail(EnrichmentFailureCause.CreditsExhausted)),
            credits = credits,
        )

        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        val failed: FailedEnrichment? = viewModel.state.value.failedEnrichment
        assertThat(failed?.action).isEqualTo(FailedEnrichmentAction.None)
        assertThat(failed?.creditsResetAt).isEqualTo(credits.resetAt)
    }

    @Test
    fun `exhausted credits offer try again once the reset has passed`() = runTest {
        val viewModel: FlashcardDetailViewModel = buildViewModel(
            FakeFlashcardReadRepo(failedDetail(EnrichmentFailureCause.CreditsExhausted)),
            credits = GenerationCredits(remaining = 0, resetAt = NOW.minusSeconds(1)),
        )

        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        assertThat(viewModel.state.value.failedEnrichment?.action).isEqualTo(FailedEnrichmentAction.TryAgain)
    }

    @Test
    fun `try again moves only this card back to pending and enqueues its enrichment`() = runTest {
        val enrichment: FlashcardEnrichmentRepository = pendingRecorder()
        val viewModel: FlashcardDetailViewModel = buildViewModel(
            FakeFlashcardReadRepo(failedDetail(EnrichmentFailureCause.Technical)),
            enrichment = enrichment,
        )
        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        viewModel.effect.test {
            viewModel.onIntent(FlashcardDetailUiIntent.TryAgainClicked)
            assertThat(awaitItem()).isEqualTo(FlashcardDetailUiEffect.EnqueueEnrichment("card-1"))
        }
        coVerify(exactly = 1) { enrichment.markPending(listOf("card-1".toFlashcardId())) }
    }

    @Test
    fun `try again before the credits reset enqueues nothing`() = runTest {
        val enrichment: FlashcardEnrichmentRepository = pendingRecorder()
        val viewModel: FlashcardDetailViewModel = buildViewModel(
            FakeFlashcardReadRepo(failedDetail(EnrichmentFailureCause.CreditsExhausted)),
            credits = GenerationCredits(remaining = 0, resetAt = NOW.plusSeconds(3_600)),
            enrichment = enrichment,
        )
        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        viewModel.effect.test {
            viewModel.onIntent(FlashcardDetailUiIntent.TryAgainClicked)
            expectNoEvents()
        }
        coVerify(exactly = 0) { enrichment.markPending(any()) }
    }

    @Test
    fun `a failed retry shows a message`() = runTest {
        val enrichment: FlashcardEnrichmentRepository = mockk()
        coEvery { enrichment.markPending(any()) } throws IllegalStateException("db closed")
        val viewModel: FlashcardDetailViewModel = buildViewModel(
            FakeFlashcardReadRepo(failedDetail(EnrichmentFailureCause.Technical)),
            enrichment = enrichment,
        )
        viewModel.onIntent(FlashcardDetailUiIntent.Load)

        viewModel.effect.test {
            viewModel.onIntent(FlashcardDetailUiIntent.TryAgainClicked)
            assertThat(awaitItem()).isEqualTo(FlashcardDetailUiEffect.ShowMessage(R.string.card_detail_retry_error))
        }
    }

    private fun buildViewModel(
        repo: FakeFlashcardReadRepo,
        credits: GenerationCredits? = null,
        enrichment: FlashcardEnrichmentRepository = pendingRecorder(),
    ): FlashcardDetailViewModel {
        val clock = Clock { NOW }
        val creditsRepository = FixedCreditsRepository(credits)
        return FlashcardDetailViewModel(
            flashcardId = "card-1",
            flashcardRepository = repo,
            generationCredits = creditsRepository,
            retryEnrichment = RetryEnrichmentUseCase(repo, enrichment, creditsRepository, clock),
            clock = clock,
            undoEventHolder = UndoEventHolder(),
        )
    }

    private fun pendingRecorder(): FlashcardEnrichmentRepository {
        val enrichment: FlashcardEnrichmentRepository = mockk()
        coEvery { enrichment.markPending(any()) } just Runs
        return enrichment
    }

    private fun detailOf(word: String): FlashcardDetail = FlashcardDetail(
        flashcard = Flashcard.empty(SystemClock).copy(
            id = "card-1".toFlashcardId(),
            word = word,
        ),
    )

    private fun failedDetail(cause: EnrichmentFailureCause): FlashcardDetail = FlashcardDetail(
        flashcard = Flashcard.empty(SystemClock).copy(
            id = "card-1".toFlashcardId(),
            word = "hello",
            enrichmentStatus = EnrichmentStatus.FAILED,
            enrichmentFailureCause = cause,
        ),
    )

    private class FixedCreditsRepository(private val credits: GenerationCredits?) : GenerationCreditsRepository {
        override fun observe(): Flow<GenerationCredits?> = flowOf(credits)
        override suspend fun record(credits: GenerationCredits) = Unit
    }

    private class FakeFlashcardReadRepo(
        initial: FlashcardDetail = FlashcardDetail(Flashcard.empty(SystemClock)),
        private val shouldFail: Boolean = false,
    ) : FlashcardRepository {
        val detail: MutableStateFlow<FlashcardDetail?> = MutableStateFlow(initial)

        override fun fetchAll(): Flow<List<Flashcard>> = emptyFlow()
        override fun fetchByDeckId(deckId: DeckId): Flow<List<Flashcard>> = emptyFlow()
        override suspend fun fetchById(id: FlashcardId): FlashcardDetail = checkNotNull(detail.value)
        override fun observeById(id: FlashcardId): Flow<FlashcardDetail?> {
            if (shouldFail) return flow { error("fetch failed") }
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

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-10T20:00:00Z")
    }
}
