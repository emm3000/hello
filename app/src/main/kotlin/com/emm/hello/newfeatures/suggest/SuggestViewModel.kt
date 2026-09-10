package com.emm.hello.newfeatures.suggest

import androidx.lifecycle.viewModelScope
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.suggestion.ObserveSuggestedWordsUseCase
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.SuggestedWordsRefresher
import com.emm.domain.suggestion.SuggestionRefreshStatus
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.hello.R
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SuggestViewModel(
    private val observeSuggestedWords: ObserveSuggestedWordsUseCase,
    private val refresher: SuggestedWordsRefresher,
    private val captureFlashcardUseCase: CaptureFlashcardUseCase,
    private val getDecksUseCase: GetDecksUseCase,
    private val defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
) : MviViewModel<SuggestUiState, SuggestUiIntent, SuggestUiEffect>(
    initialState = SuggestUiState(),
) {

    init {
        refresher.ensure()
        observeSuggestions()
    }

    override fun onIntent(intent: SuggestUiIntent) {
        when (intent) {
            SuggestUiIntent.Retry -> refresher.refresh()
            is SuggestUiIntent.WordToggled -> toggleWord(intent.word)
            SuggestUiIntent.AddSelected -> handleAddSelected()
            SuggestUiIntent.BackClicked -> sendEffect(SuggestUiEffect.NavigateBack)
        }
    }

    private fun observeSuggestions() = viewModelScope.launch {
        combine(observeSuggestedWords(), refresher.status, ::Pair).collect { (pool, status) ->
            render(pool, status)
        }
    }

    private fun render(pool: WordSuggestions?, status: SuggestionRefreshStatus) {
        val words: List<SuggestedWord> = pool?.words.orEmpty()
        val hasWords: Boolean = words.isNotEmpty()
        if (status is SuggestionRefreshStatus.Failed) {
            logError(TAG, "refresh:error ${status.error.message}", status.error)
        }
        setState {
            copy(
                isLoading = !hasWords && status.isDecisionPending(),
                loadFailed = !hasWords && status is SuggestionRefreshStatus.Failed,
                isOffline = !hasWords && status is SuggestionRefreshStatus.Offline,
                situation = pool?.situation.orEmpty(),
                words = words,
                selectedWords = selectedWords.filterTo(mutableSetOf()) { selected ->
                    words.any { it.word == selected }
                },
            )
        }
    }

    private fun toggleWord(word: String) {
        setState {
            copy(selectedWords = if (word in selectedWords) selectedWords - word else selectedWords + word)
        }
    }

    private fun resolveTargetDeck(decks: List<Deck>): Deck? {
        val defaultDeckId: DeckId? = defaultDeckSelectionRepository.getDefaultDeckId()
        return decks.find { it.id == defaultDeckId } ?: decks.minByOrNull(Deck::createdAt)
    }

    private fun handleAddSelected() = viewModelScope.launch {
        val current: SuggestUiState = currentState
        if (!current.canAdd) return@launch

        setState { copy(isAdding = true) }
        try {
            addSelectedWords(current)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logError(TAG, "addSelected:error ${error.message}", error)
            sendEffect(SuggestUiEffect.ShowMessage(R.string.suggest_error_add))
        } finally {
            setState { copy(isAdding = false) }
        }
    }

    private suspend fun addSelectedWords(current: SuggestUiState) {
        val decks: List<Deck> = getDecksUseCase().first()
        val deck: Deck? = resolveTargetDeck(decks)
        if (deck == null) {
            sendEffect(SuggestUiEffect.ShowMessage(R.string.suggest_error_no_deck))
            return
        }

        val selectedWords: List<SuggestedWord> = current.words.filter { it.word in current.selectedWords }
        val flashcardIds: List<String> = selectedWords.mapNotNull { captureOrSkip(deck.id, it) }
        if (flashcardIds.isEmpty()) {
            reportAllWordsAlreadyKnown()
        } else {
            reportWordsAdded(flashcardIds)
        }
    }

    private fun reportAllWordsAlreadyKnown() {
        setState { copy(selectedWords = emptySet()) }
        sendEffect(SuggestUiEffect.ShowMessage(R.string.suggest_all_known))
    }

    private fun reportWordsAdded(flashcardIds: List<String>) {
        sendEffect(SuggestUiEffect.EnqueueEnrichment(flashcardIds))
        sendEffect(SuggestUiEffect.ShowMessage(R.string.suggest_added))
        sendEffect(SuggestUiEffect.NavigateBack)
    }

    private suspend fun captureOrSkip(deckId: DeckId, suggestedWord: SuggestedWord): String? {
        return try {
            captureFlashcardUseCase(
                deckId = deckId,
                word = suggestedWord.word,
                translation = suggestedWord.translation,
            ).value
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (validation: DomainValidationException) {
            if (validation.isDuplicate()) null else throw validation
        }
    }
}

private fun SuggestionRefreshStatus.isDecisionPending(): Boolean =
    this is SuggestionRefreshStatus.Idle || this is SuggestionRefreshStatus.Running

private const val TAG = "SuggestViewModel"

private fun DomainValidationException.isDuplicate(): Boolean =
    issues.any { it.code == IssueCode.DuplicateWordInDeck }
