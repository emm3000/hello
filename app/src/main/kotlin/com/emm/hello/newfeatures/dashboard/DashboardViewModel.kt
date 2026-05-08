package com.emm.hello.newfeatures.dashboard

import androidx.lifecycle.viewModelScope
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.hello.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DashboardViewModel(
    getDecksUseCase: GetDecksUseCase,
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
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

    fun onVisible() {
        viewModelScope.launch {
            val stats = getDashboardStatsUseCase()
            mutableState.value = mutableState.value.copy(stats = stats)
        }
    }

    override fun onIntent(intent: DashboardUiIntent) = Unit
}
