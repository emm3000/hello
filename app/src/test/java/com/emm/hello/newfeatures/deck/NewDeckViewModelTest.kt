package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.CreateDeckUseCase
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class NewDeckViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit success resets state and emits navigate back effect`() = runTest {
        val repository = FakeDeckRepository()
        val viewModel = NewDeckViewModel(CreateDeckUseCase(repository))

        viewModel.onIntent(NewDeckUiIntent.NameChanged("My deck"))
        viewModel.onIntent(NewDeckUiIntent.DescriptionChanged("Optional"))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(NewDeckUiIntent.Submit)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(NewDeckUiEffect.NavigateBack)
        assertThat(viewModel.uiState.value.name).isEmpty()
        assertThat(viewModel.uiState.value.description).isEmpty()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(repository.lastAdded).isEqualTo(CreateDeckInput("My deck", "Optional"))
    }

    @Test
    fun `submit failure emits show message and stops loading`() = runTest {
        val repository = FakeDeckRepository(shouldFail = true)
        val viewModel = NewDeckViewModel(CreateDeckUseCase(repository))

        viewModel.onIntent(NewDeckUiIntent.NameChanged("Deck with error"))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(NewDeckUiIntent.Submit)

        val effect = effectDeferred.await()
        assertThat(effect).isInstanceOf(NewDeckUiEffect.ShowMessage::class.java)
        assertThat((effect as NewDeckUiEffect.ShowMessage).message).isEqualTo("boom")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(repository.addDeckCalls).isEqualTo(1)
    }

    private class FakeDeckRepository(
        private val shouldFail: Boolean = false,
    ) : DeckRepository {
        var addDeckCalls: Int = 0
        var lastAdded: CreateDeckInput? = null

        override suspend fun addDeck(deck: CreateDeckInput) {
            addDeckCalls += 1
            lastAdded = deck
            if (shouldFail) error("boom")
        }

        override fun findById(deckId: String): Flow<Deck> = emptyFlow()

        override fun fetchAll(): Flow<List<Deck>> = emptyFlow()

        override fun deckWithFlashcardCount(): Flow<List<Deck>> = emptyFlow()
    }
}
