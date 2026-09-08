package com.emm.hello.newfeatures.store

import com.emm.hello.core.mvi.MviState

data class StoreUiState(
    val isLoading: Boolean = true,
    val decks: List<StoreDeckItem> = emptyList(),
    val installingDeckId: String? = null,
) : MviState
