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
        runCatching {
            deckRepository.fetchById(deckId.toDeckId()).first()
        }.onSuccess { deck ->
            if (deck == null) {
                setState { copy(isLoading = false) }
                sendEffect(NewDeckUiEffect.ShowMessage("No se pudo cargar el mazo"))
                return@onSuccess
            }
            setState {
                copy(
                    name = deck.name,
                    description = deck.description,
                    tags = deck.tags.map { tag -> tag.value },
                    isLoading = false,
                )
            }
        }.onFailure { error ->
            logError(TAG, "loadDeck:error ${error.message}", error)
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
        runCatching {
            val input = CreateDeckInput(
                name = state.name,
                description = state.description,
                tags = state.tags,
            )
            deckRepository.create(input)
        }.onSuccess {
            setState { NewDeckUiState() }
            sendEffect(NewDeckUiEffect.NavigateBack)
        }.onFailure { error ->
            logError(TAG, "createDeck:error ${error.message}", error)
            setState { copy(isLoading = false) }
            sendEffect(NewDeckUiEffect.ShowMessage("No se pudo crear el mazo"))
        }
    }

    private suspend fun updateDeck(state: NewDeckUiState) {
        val deckId = (state.formMode as DeckFormMode.Edit).deckId
        runCatching {
            updateDeckUseCase(
                UpdateDeckInput(
                    deckId = deckId.toDeckId(),
                    name = state.name,
                    description = state.description,
                    tags = state.tags,
                )
            )
        }.onSuccess {
            sendEffect(NewDeckUiEffect.NavigateBack)
        }.onFailure { error ->
            logError(TAG, "updateDeck:error ${error.message}", error)
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
