package com.emm.hello.newfeatures.deck

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.DeckRepository
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.logging.logError
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewDeckViewModel(
    private val deckRepository: DeckRepository,
) : MviViewModel<NewDeckUiState, NewDeckUiIntent, NewDeckUiEffect>(
    initialState = NewDeckUiState(),
) {

    override fun onIntent(intent: NewDeckUiIntent) {
        when (intent) {
            is NewDeckUiIntent.DescriptionChanged -> mutableState.update { it.copy(description = intent.description) }
            is NewDeckUiIntent.NameChanged -> mutableState.update { it.copy(name = intent.name) }
            NewDeckUiIntent.Submit -> createDeck()
        }
    }

    private fun createDeck() = viewModelScope.launch {
        val current = mutableState.value
        if (!current.isValid || current.isLoading) return@launch

        mutableState.update { it.copy(isLoading = true) }
        runCatching {
            val input = CreateDeckInput(
                name = current.name,
                description = current.description,
            )
            deckRepository.addDeck(input)
        }.onSuccess {
            mutableState.update { NewDeckUiState() }
            mutableEffect.send(NewDeckUiEffect.NavigateBack)
        }.onFailure { error ->
            logError(TAG, "createDeck:error ${error.message}", error)
            mutableState.update { it.copy(isLoading = false) }
            mutableEffect.send(NewDeckUiEffect.ShowMessage(error.message ?: "No se pudo crear el mazo"))
        }
    }
}

private const val TAG = "NewDeckViewModel"
