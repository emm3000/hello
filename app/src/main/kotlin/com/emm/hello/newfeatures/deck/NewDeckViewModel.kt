package com.emm.hello.newfeatures.deck

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.UpdateDeckInput
import com.emm.domain.deck.UpdateDeckUseCase
import com.emm.domain.ids.toDeckId
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class NewDeckViewModel(
    private val deckRepository: DeckRepository,
    private val updateDeckUseCase: UpdateDeckUseCase,
    formMode: DeckFormMode,
) : MviViewModel<NewDeckUiState, NewDeckUiIntent, NewDeckUiEffect>(
    initialState = NewDeckUiState(formMode = formMode),
) {

    init {
        if (currentState.formMode is DeckFormMode.Edit) {
            loadDeck()
        }
    }

    override fun onIntent(intent: NewDeckUiIntent) {
        when (intent) {
            is NewDeckUiIntent.NameChanged -> setState { copy(name = intent.name) }
            is NewDeckUiIntent.DescriptionChanged -> setState { copy(description = intent.description) }
            is NewDeckUiIntent.TagsChanged -> setState { copy(tags = intent.tags.normalizeTags()) }
            NewDeckUiIntent.Submit -> handleSubmit()
        }
    }

    private fun loadDeck() = viewModelScope.launch {
        val deckId = (currentState.formMode as DeckFormMode.Edit).deckId
        setState { copy(isLoading = true) }
        try {
            val deck = deckRepository.fetchById(deckId.toDeckId()).first()
            if (deck == null) {
                setState { copy(isLoading = false) }
                sendEffect(NewDeckUiEffect.ShowMessage("No se pudo cargar el mazo"))
                return@launch
            }
            setState {
                copy(
                    name = deck.name,
                    description = deck.description,
                    tags = deck.tags.map { tag -> tag.value },
                    isLoading = false,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "loadDeck:error ${e.message}", e)
            setState { copy(isLoading = false) }
            sendEffect(NewDeckUiEffect.ShowMessage("No se pudo cargar el mazo"))
        }
    }

    private fun handleSubmit() = viewModelScope.launch {
        val current = currentState
        if (!current.isValid || current.isLoading) return@launch

        setState { copy(isLoading = true) }
        when (current.formMode) {
            DeckFormMode.Create -> createDeck(current)
            is DeckFormMode.Edit -> updateDeck(current)
        }
    }

    private suspend fun createDeck(state: NewDeckUiState) {
        try {
            deckRepository.create(
                CreateDeckInput(
                    name = state.name,
                    description = state.description,
                    tags = state.tags,
                )
            )
            setState { NewDeckUiState() }
            sendEffect(NewDeckUiEffect.NavigateBack)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "createDeck:error ${e.message}", e)
            setState { copy(isLoading = false) }
            sendEffect(NewDeckUiEffect.ShowMessage("No se pudo crear el mazo"))
        }
    }

    private suspend fun updateDeck(state: NewDeckUiState) {
        val deckId = (state.formMode as DeckFormMode.Edit).deckId
        try {
            updateDeckUseCase(
                UpdateDeckInput(
                    deckId = deckId.toDeckId(),
                    name = state.name,
                    description = state.description,
                    tags = state.tags,
                )
            )
            sendEffect(NewDeckUiEffect.NavigateBack)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(TAG, "updateDeck:error ${e.message}", e)
            setState { copy(isLoading = false) }
            sendEffect(NewDeckUiEffect.ShowMessage("No se pudo actualizar el mazo"))
        }
    }
}

private const val TAG = "NewDeckViewModel"

private fun List<String>.normalizeTags(): List<String> =
    map { it.lowercase().trim() }
        .filter { it.isNotBlank() }
        .distinct()
