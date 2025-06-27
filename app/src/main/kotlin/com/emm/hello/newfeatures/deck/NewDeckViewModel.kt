package com.emm.hello.newfeatures.deck

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.deck.DeckCreator
import kotlinx.coroutines.launch

data class NewDeckUiState(
    val name: String = "",
    val description: String = ""
)

sealed interface NewDeckAction {

    data class NameChanged(val name: String) : NewDeckAction
    data class DescriptionChanged(val description: String) : NewDeckAction
    object Submit : NewDeckAction
}

class NewDeckViewModel(private val deckCreator: DeckCreator) : ViewModel() {

    var state by mutableStateOf(NewDeckUiState())
        private set

    fun onAction(action: NewDeckAction) {
        when (action) {
            is NewDeckAction.DescriptionChanged -> state = state.copy(description = action.description)
            is NewDeckAction.NameChanged -> state = state.copy(name = action.name)
            NewDeckAction.Submit -> createDeck()
        }
    }

    private fun createDeck() = viewModelScope.launch {
        val input = CreateDeckInput(
            name = state.name,
            description = state.description
        )
        deckCreator.create(input)
    }
}