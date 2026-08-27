package com.emm.hello.newfeatures.hoy

import androidx.lifecycle.viewModelScope
import com.emm.domain.study.DashboardStats
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.newfeatures.study.StudyRoute
import kotlinx.coroutines.launch

class HoyViewModel(
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
) : MviViewModel<HoyUiState, HoyUiIntent, HoyUiEffect>(
    initialState = HoyUiState(isLoading = true),
) {

    override fun onIntent(intent: HoyUiIntent) {
        when (intent) {
            ScreenVisible -> loadStats()
            StudyClicked -> sendEffect(NavigateToStudy(StudyRoute.ALL_DUE_DECKS))
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val stats: DashboardStats = getDashboardStatsUseCase()
            setState { copy(stats = stats, isLoading = false) }
        }
    }
}
