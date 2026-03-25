package com.emm.hello.newfeatures.deck

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.CreateDeckUseCase
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewDeckViewModel(
    private val createDeckUseCase: CreateDeckUseCase,
    private val ensureLinkedIdentityUseCase: EnsureLinkedIdentityUseCase,
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
            ensureLinkedIdentityUseCase()
            val input = CreateDeckInput(
                name = current.name,
                description = current.description,
            )
            createDeckUseCase(input)
        }.onSuccess {
            mutableState.update { NewDeckUiState() }
            mutableEffect.send(NewDeckUiEffect.NavigateBack)
        }.onFailure { error ->
            mutableState.update { it.copy(isLoading = false) }
            mutableEffect.send(NewDeckUiEffect.ShowMessage(error.message ?: "No se pudo crear el mazo"))
        }
    }
}
