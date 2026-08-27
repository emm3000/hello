package com.emm.hello.newfeatures.library

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.Deck
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.flashcard.RestoreFlashcardUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.library.LibraryFlashcard
import com.emm.domain.library.SearchLibraryUseCase
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.R
import com.emm.hello.logging.logError
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 200L

class LibraryViewModel(
    private val searchLibrary: SearchLibraryUseCase,
    private val restoreFlashcardUseCase: RestoreFlashcardUseCase,
    undoEventHolder: UndoEventHolder,
    getDecksUseCase: GetDecksUseCase,
) : MviViewModel<LibraryUiState, LibraryUiIntent, LibraryUiEffect>(
    initialState = LibraryUiState(),
) {

    private val criteria: MutableStateFlow<LibrarySearchCriteria> = MutableStateFlow(LibrarySearchCriteria())

    init {
        getDecksUseCase()
            .onEach { decks: List<Deck> -> setState { copy(decks = decks) } }
            .launchIn(viewModelScope)

        criteria
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .flatMapLatest { current -> searchLibrary(query = current.query, deckId = current.deckId) }
            .onEach { cards: List<LibraryFlashcard> -> setState { copy(cards = cards, isLoading = false) } }
            .launchIn(viewModelScope)

        undoEventHolder.events
            .filterIsInstance<UndoEvent.CardDeleted>()
            .onEach { event ->
                sendEffect(
                    LibraryUiEffect.ShowUndoCardDeleted(
                        flashcardId = event.flashcardId,
                        deletedAt = event.deletedAt,
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: LibraryUiIntent) {
        when (intent) {
            is LibraryUiIntent.QueryChanged -> {
                setState { copy(query = intent.value) }
                criteria.update { current -> current.copy(query = intent.value) }
            }

            is LibraryUiIntent.DeckFilterToggled -> {
                val next: DeckId? = if (currentState.selectedDeckId == intent.deckId) null else intent.deckId
                setState { copy(selectedDeckId = next) }
                criteria.update { current -> current.copy(deckId = next) }
            }

            LibraryUiIntent.FiltersCleared -> {
                setState { copy(query = "", selectedDeckId = null) }
                criteria.update { LibrarySearchCriteria() }
            }

            is LibraryUiIntent.CardOpened -> sendEffect(
                LibraryUiEffect.OpenCard(
                    cardId = intent.card.id.value,
                    deckId = intent.card.deckId.value,
                ),
            )

            LibraryUiIntent.CaptureRequested -> sendEffect(LibraryUiEffect.OpenCapture)

            is LibraryUiIntent.UndoDeleteCard -> undoDeleteCard(intent.flashcardId, intent.deletedAt)
        }
    }

    private fun undoDeleteCard(flashcardId: String, deletedAt: Long) = viewModelScope.launch {
        try {
            restoreFlashcardUseCase(flashcardId.toFlashcardId(), deletedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "undoDeleteCard:error ${e.message}", e)
            sendEffect(LibraryUiEffect.ShowMessage(R.string.error_restore_card))
        }
    }
}

private const val TAG = "LibraryViewModel"
