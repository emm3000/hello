package com.emm.hello.newfeatures.deck

import com.emm.hello.core.mvi.MviState

data class NewDeckUiState(
    val name: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val formMode: DeckFormMode = DeckFormMode.Create,
    val isDeleteConfirmationVisible: Boolean = false,
) : MviState {

    val isValid: Boolean
        get() = name.isNotBlank()

    val canDelete: Boolean
        get() = formMode is DeckFormMode.Edit && !isLoading
}
