package com.emm.hello.newfeatures.deck

import com.emm.hello.core.mvi.MviState

data class NewDeckUiState(
    val name: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val formMode: DeckFormMode = DeckFormMode.Create,
) : MviState {

    val isValid: Boolean
        get() = name.isNotBlank()
}
