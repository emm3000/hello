package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class DashboardViewModel(
    getDecksUseCase: GetDecksUseCase,
) : MviViewModel<DashboardUiState, DashboardUiIntent, DashboardUiEffect>(
    initialState = DashboardUiState(isLoading = true),
) {

    init {
        getDecksUseCase()
            .map { decks ->
                mutableState.value.copy(
                    decks = decks,
                    isLoading = false,
                )
            }
            .onEach { mutableState.value = it }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: DashboardUiIntent) = Unit
}
