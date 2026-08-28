package com.emm.hello.newfeatures.today

import androidx.lifecycle.viewModelScope
import com.emm.domain.study.DashboardStats
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.newfeatures.study.StudyRoute
import kotlinx.coroutines.launch

class TodayViewModel(
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
) : MviViewModel<TodayUiState, TodayUiIntent, TodayUiEffect>(
    initialState = TodayUiState(isLoading = true),
) {

    override fun onIntent(intent: TodayUiIntent) {
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
