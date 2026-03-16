package com.emm.hello.newfeatures.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.CreateDeckUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewDeckViewModel(private val createDeckUseCase: CreateDeckUseCase) : ViewModel() {

    private val _state = MutableStateFlow(NewDeckUiState())
    val state = _state.asStateFlow()

    private val _effect = Channel<NewDeckUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: NewDeckUiIntent) {
        when (intent) {
            is NewDeckUiIntent.DescriptionChanged -> _state.update { it.copy(description = intent.description) }
            is NewDeckUiIntent.NameChanged -> _state.update { it.copy(name = intent.name) }
            NewDeckUiIntent.Submit -> createDeck()
        }
    }

    private fun createDeck() = viewModelScope.launch {
        val current = _state.value
        if (!current.isValid || current.isLoading) return@launch

        _state.update { it.copy(isLoading = true) }
        runCatching {
            val input = CreateDeckInput(
                name = current.name,
                description = current.description,
            )
            createDeckUseCase.create(input)
        }.onSuccess {
            _state.update { NewDeckUiState() }
            _effect.send(NewDeckUiEffect.NavigateBack)
        }.onFailure { error ->
            _state.update { it.copy(isLoading = false) }
            _effect.send(NewDeckUiEffect.ShowMessage(error.message ?: "No se pudo crear el mazo"))
        }
    }
}
