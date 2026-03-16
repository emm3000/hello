package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.CreateDeckUseCase
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.hello.MainDispatcherRule
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertEquals(NewDeckUiEffect.NavigateBack, effect)
        assertEquals("", viewModel.uiState.value.name)
        assertEquals("", viewModel.uiState.value.description)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(CreateDeckInput("My deck", "Optional"), repository.lastAdded)
    }

    @Test
    fun `submit failure emits show message and stops loading`() = runTest {
        val repository = FakeDeckRepository(shouldFail = true)
        val viewModel = NewDeckViewModel(CreateDeckUseCase(repository))

        viewModel.onIntent(NewDeckUiIntent.NameChanged("Deck with error"))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(NewDeckUiIntent.Submit)

        val effect = effectDeferred.await()
        assertTrue(effect is NewDeckUiEffect.ShowMessage)
        assertEquals("boom", (effect as NewDeckUiEffect.ShowMessage).message)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, repository.addDeckCalls)
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
