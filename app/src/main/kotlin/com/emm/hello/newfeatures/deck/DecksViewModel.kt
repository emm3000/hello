package com.emm.hello.newfeatures.deck

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.Deck
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.RestoreDeckUseCase
import com.emm.domain.ids.toDeckId
import com.emm.hello.R
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DecksViewModel(
    private val restoreDeckUseCase: RestoreDeckUseCase,
    undoEventHolder: UndoEventHolder,
    getDecksUseCase: GetDecksUseCase,
) : MviViewModel<DecksUiState, DecksUiIntent, DecksUiEffect>(
    initialState = DecksUiState(),
) {

    init {
        getDecksUseCase()
            .onEach { decks: List<Deck> -> setState { copy(decks = decks, isLoading = false) } }
            .launchIn(viewModelScope)

        undoEventHolder.events
            .filterIsInstance<UndoEvent.DeckDeleted>()
            .onEach { event ->
                sendEffect(
                    DecksUiEffect.ShowUndoDeckDeleted(
                        deckName = event.deckName,
                        deckId = event.deckId,
                        deletedAt = event.deletedAt,
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: DecksUiIntent) {
        when (intent) {
            is DecksUiIntent.DeckOpened -> sendEffect(DecksUiEffect.OpenDeckForm(intent.deckId))
            DecksUiIntent.CreateDeckRequested -> sendEffect(DecksUiEffect.OpenDeckForm(null))
            is DecksUiIntent.UndoDeleteDeck -> undoDeleteDeck(intent.deckId, intent.deletedAt)
        }
    }

    private fun undoDeleteDeck(deckId: String, deletedAt: Long) = viewModelScope.launch {
        try {
            restoreDeckUseCase(deckId.toDeckId(), deletedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "undoDeleteDeck:error ${e.message}", e)
            sendEffect(DecksUiEffect.ShowMessage(R.string.error_restore_deck))
        }
    }
}

private const val TAG = "DecksViewModel"
