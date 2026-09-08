package com.emm.hello.newfeatures.store

import androidx.lifecycle.viewModelScope
import com.emm.domain.curated.CuratedDeckListing
import com.emm.domain.curated.GetCuratedDecksUseCase
import com.emm.domain.curated.InstallCuratedDeckUseCase
import com.emm.hello.R
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class StoreViewModel(
    getCuratedDecksUseCase: GetCuratedDecksUseCase,
    private val installCuratedDeckUseCase: InstallCuratedDeckUseCase,
) : MviViewModel<StoreUiState, StoreUiIntent, StoreUiEffect>(
    initialState = StoreUiState(),
) {

    init {
        getCuratedDecksUseCase()
            .onEach { listings: List<CuratedDeckListing> ->
                setState { copy(isLoading = false, decks = listings.map(CuratedDeckListing::toItem)) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: StoreUiIntent) {
        when (intent) {
            StoreUiIntent.BackRequested -> sendEffect(StoreUiEffect.NavigateBack)
            is StoreUiIntent.InstallRequested -> install(intent.curatedDeckId)
            is StoreUiIntent.OpenDeckRequested -> sendEffect(StoreUiEffect.OpenDeck(intent.deckId))
        }
    }

    private fun install(curatedDeckId: String) {
        if (currentState.installingDeckId == curatedDeckId) return
        setState { copy(installingDeckId = curatedDeckId) }
        viewModelScope.launch {
            try {
                installCuratedDeckUseCase(curatedDeckId)
                setState { copy(installingDeckId = null) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                logError(TAG, "install:error ${failure.message}", failure)
                setState { copy(installingDeckId = null) }
                sendEffect(StoreUiEffect.ShowMessage(R.string.store_install_failed))
            }
        }
    }
}

private fun CuratedDeckListing.toItem(): StoreDeckItem = StoreDeckItem(
    id = deck.id,
    name = deck.name,
    description = deck.description,
    tags = deck.tags,
    levelBand = deck.levelBand,
    cardsCount = deck.notes.size,
    installedDeckId = installedDeckId?.value,
)

private const val TAG = "StoreViewModel"
