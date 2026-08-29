package com.emm.hello.newfeatures.suggest

import com.emm.domain.suggestion.SuggestedWord
import com.emm.hello.core.mvi.MviState

data class SuggestUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val situation: String = "",
    val words: List<SuggestedWord> = emptyList(),
    val selectedWords: Set<String> = emptySet(),
    val isAdding: Boolean = false,
) : MviState {

    val selectedCount: Int
        get() = selectedWords.size

    val canAdd: Boolean
        get() = selectedCount > 0 && !isAdding
}
