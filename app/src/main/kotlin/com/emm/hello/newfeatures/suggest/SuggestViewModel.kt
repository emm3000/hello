package com.emm.hello.newfeatures.suggest

import androidx.lifecycle.viewModelScope
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.connectivity.ConnectivityRepository
import com.emm.domain.deck.Deck
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.ids.DeckId
import com.emm.domain.suggestion.SuggestWordsUseCase
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.validation.DomainValidationException
import com.emm.domain.validation.IssueCode
import com.emm.hello.R
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SuggestViewModel(
    private val suggestWordsUseCase: SuggestWordsUseCase,
    private val captureFlashcardUseCase: CaptureFlashcardUseCase,
    private val getDecksUseCase: GetDecksUseCase,
    private val defaultDeckSelectionRepository: DefaultDeckSelectionRepository,
    private val connectivityRepository: ConnectivityRepository,
) : MviViewModel<SuggestUiState, SuggestUiIntent, SuggestUiEffect>(
    initialState = SuggestUiState(),
) {

    init {
        load()
    }

    override fun onIntent(intent: SuggestUiIntent) {
        when (intent) {
            SuggestUiIntent.Retry -> load()
            is SuggestUiIntent.WordToggled -> toggleWord(intent.word)
            SuggestUiIntent.AddSelected -> handleAddSelected()
            SuggestUiIntent.BackClicked -> sendEffect(SuggestUiEffect.NavigateBack)
        }
    }

    private fun load() = viewModelScope.launch {
        setState { copy(isLoading = true, loadFailed = false, isOffline = false) }
        if (!isOnline()) {
            setState { copy(isLoading = false, isOffline = true) }
            return@launch
        }
        try {
            val suggestions: WordSuggestions = suggestWordsUseCase()
            setState {
                copy(
                    isLoading = false,
                    situation = suggestions.situation,
                    words = suggestions.words,
                    selectedWords = emptySet(),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logError(TAG, "load:error ${error.message}", error)
            setState { copy(isLoading = false, loadFailed = true) }
        }
    }

    private suspend fun isOnline(): Boolean = connectivityRepository.observeOnline().first()

    private fun toggleWord(word: String) {
        setState {
            copy(selectedWords = if (word in selectedWords) selectedWords - word else selectedWords + word)
        }
    }

    private fun resolveTargetDeck(decks: List<Deck>): Deck? {
        val defaultDeckId: DeckId? = defaultDeckSelectionRepository.getDefaultDeckId()
        return decks.find { it.id == defaultDeckId } ?: decks.firstOrNull()
    }

    private fun handleAddSelected() = viewModelScope.launch {
        val current: SuggestUiState = currentState
        if (!current.canAdd) return@launch

        setState { copy(isAdding = true) }
        val decks: List<Deck> = getDecksUseCase().first()
        val deck: Deck? = resolveTargetDeck(decks)
        if (deck == null) {
            setState { copy(isAdding = false) }
            sendEffect(SuggestUiEffect.ShowMessage(R.string.suggest_error_no_deck))
            return@launch
        }

        addSelectedWords(current, deck.id)
    }

    private suspend fun addSelectedWords(current: SuggestUiState, deckId: DeckId) {
        try {
            val selectedWords: List<SuggestedWord> = current.words.filter { it.word in current.selectedWords }
            val flashcardIds: List<String> = selectedWords.mapNotNull { captureOrSkip(deckId, it) }
            if (flashcardIds.isNotEmpty()) {
                sendEffect(SuggestUiEffect.EnqueueEnrichment(flashcardIds))
            }
            sendEffect(SuggestUiEffect.ShowMessage(R.string.suggest_added))
            sendEffect(SuggestUiEffect.NavigateBack)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logError(TAG, "addSelectedWords:error ${error.message}", error)
            setState { copy(isAdding = false) }
            sendEffect(SuggestUiEffect.ShowMessage(R.string.suggest_error_add))
        }
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

private const val TAG = "SuggestViewModel"

private fun DomainValidationException.isDuplicate(): Boolean =
    issues.any { it.code == IssueCode.DuplicateWordInDeck }
