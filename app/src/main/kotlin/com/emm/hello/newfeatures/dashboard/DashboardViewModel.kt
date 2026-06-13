package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.DeckSearchCriteria
import com.emm.domain.deck.GetFilteredDecksUseCase
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.RestoreDeckUseCase
import com.emm.domain.ids.toDeckId
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import com.emm.hello.newfeatures.shared.UndoEvent
import com.emm.hello.newfeatures.shared.UndoEventHolder
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 120L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    getDecksUseCase: GetDecksUseCase,
    private val getFilteredDecksUseCase: GetFilteredDecksUseCase,
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val restoreDeckUseCase: RestoreDeckUseCase,
    private val undoEventHolder: UndoEventHolder,
) : MviViewModel<DashboardUiState, DashboardUiIntent, DashboardUiEffect>(
    initialState = DashboardUiState(isLoading = true),
) {

    private val criteria = MutableStateFlow(DeckSearchCriteria())

    init {
        // Collect deck-deleted undo events emitted by DeckDetailViewModel after a soft-delete.
        undoEventHolder.events
            .filterIsInstance<UndoEvent.DeckDeleted>()
            .onEach { event ->
                sendEffect(
                    ShowUndoDeckDeleted(
                        deckName = event.deckName,
                        deckId = event.deckId,
                        deletedAt = event.deletedAt,
                    )
                )
            }
            .launchIn(viewModelScope)

        combine(
            flow = getDecksUseCase(),
            flow2 = criteria
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .flatMapLatest(getFilteredDecksUseCase::invoke),
            transform = ::buildState,
        )
            .onEach { newState -> setState { newState } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: DashboardUiIntent) {
        when (intent) {
            ScreenVisible -> loadStats()

            is QueryChanged -> {
                setState { copy(searchQuery = intent.value) }
                criteria.update { current -> current.copy(query = intent.value) }
            }

            is TagToggled -> {
                val normalizedTag = intent.tag.trim().lowercase()
                val selectedTags = currentState.selectedTags.toMutableSet()
                if (selectedTags.contains(normalizedTag)) {
                    selectedTags.remove(normalizedTag)
                } else {
                    selectedTags.add(normalizedTag)
                }

                setState { copy(selectedTags = selectedTags) }
                criteria.update { current -> current.copy(selectedTags = selectedTags) }
            }

            ClearFilters -> {
                setState { copy(searchQuery = "", selectedTags = emptySet()) }
                criteria.value = DeckSearchCriteria()
            }

            StudyClicked -> {
                // No per-deck due counts in dashboard state; falls back to first deck
                val target = currentState.allDecks.firstOrNull() ?: return
                sendEffect(NavigateToStudy(target.id.value))
            }

            is UndoDeleteDeck -> undoDeleteDeck(intent.deckId, intent.deletedAt)
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val stats = getDashboardStatsUseCase()
            setState { copy(stats = stats) }
        }
    }

    private fun undoDeleteDeck(deckId: String, deletedAt: Long) = viewModelScope.launch {
        try {
            restoreDeckUseCase(deckId.toDeckId(), deletedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "undoDeleteDeck:error ${e.message}", e)
        }
        // On success the reactive deck list refreshes automatically via getDecksUseCase Flow.
    }

    private fun buildState(
        allDecks: List<com.emm.domain.deck.Deck>,
        filteredDecks: List<com.emm.domain.deck.Deck>,
    ): DashboardUiState {
        val availableTags = allDecks
            .flatMap { deck -> deck.tags.map { tag -> tag.value } }
            .distinct()
            .sorted()

        val current = currentState
        val isFiltering = current.searchQuery.isNotBlank() || current.selectedTags.isNotEmpty()
        return current.copy(
            decks = filteredDecks,
            allDecks = allDecks,
            totalDeckCount = allDecks.size,
            availableTags = availableTags,
            isLoading = false,
            isFiltering = isFiltering,
            emptyState = resolveEmptyState(
                totalDeckCount = allDecks.size,
                filteredDeckCount = filteredDecks.size,
                isFiltering = isFiltering,
            ),
        )
    }

    private fun resolveEmptyState(
        totalDeckCount: Int,
        filteredDeckCount: Int,
        isFiltering: Boolean,
    ): DashboardEmptyState {
        if (totalDeckCount == 0) return DashboardEmptyState.LibraryEmpty
        if (filteredDeckCount == 0 && isFiltering) return DashboardEmptyState.NoResults
        return DashboardEmptyState.None
    }
}

private const val TAG = "DashboardViewModel"
