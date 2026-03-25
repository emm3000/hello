package com.emm.hello.newfeatures.deck

import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.CreateDeckUseCase
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DeckRepository
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.domain.sync.PairingRepository
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
        val pairingRepository = FakePairingRepository()
        val viewModel = NewDeckViewModel(
            CreateDeckUseCase(repository),
            EnsureLinkedIdentityUseCase(pairingRepository),
        )

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
        assertThat(pairingRepository.ensureCalls).isEqualTo(1)
    }

    @Test
    fun `submit failure emits show message and stops loading`() = runTest {
        val repository = FakeDeckRepository(shouldFail = true)
        val pairingRepository = FakePairingRepository()
        val viewModel = NewDeckViewModel(
            CreateDeckUseCase(repository),
            EnsureLinkedIdentityUseCase(pairingRepository),
        )

        viewModel.onIntent(NewDeckUiIntent.NameChanged("Deck with error"))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(NewDeckUiIntent.Submit)

        val effect = effectDeferred.await()
        assertThat(effect).isInstanceOf(NewDeckUiEffect.ShowMessage::class.java)
        assertThat((effect as NewDeckUiEffect.ShowMessage).message).isEqualTo("boom")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(repository.addDeckCalls).isEqualTo(1)
        assertThat(pairingRepository.ensureCalls).isEqualTo(1)
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

    private class FakePairingRepository : PairingRepository {
        var ensureCalls: Int = 0

        override suspend fun ensureLinkedIdentity() {
            ensureCalls += 1
        }

        override suspend fun createPairingSession(ttlMinutes: Int) = error("unused")

        override suspend fun redeemPairingCode(code: String) = Unit

        override suspend fun listLinkedDevices() = emptyList<com.emm.domain.sync.LinkedDevice>()

        override suspend fun revokeLinkedDevice(deviceId: String, reason: String?) = false
    }
}
