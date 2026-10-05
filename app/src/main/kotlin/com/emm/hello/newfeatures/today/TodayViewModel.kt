package com.emm.hello.newfeatures.today

import androidx.lifecycle.viewModelScope
import com.emm.domain.study.DashboardStats
import com.emm.domain.study.EXTRA_NEW_CARDS_PER_REQUEST
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.hello.analytics.ExtraNewCardsSource
import com.emm.hello.analytics.ProductAnalytics
import com.emm.hello.analytics.ProductEvent
import com.emm.hello.core.mvi.MviViewModel
import com.emm.hello.newfeatures.study.StudyRoute
import kotlinx.coroutines.launch

class TodayViewModel(
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val productAnalytics: ProductAnalytics,
) : MviViewModel<TodayUiState, TodayUiIntent, TodayUiEffect>(
    initialState = TodayUiState(isLoading = true),
) {

    override fun onIntent(intent: TodayUiIntent) {
        when (intent) {
            ScreenVisible -> loadStats()
            StudyClicked -> sendEffect(NavigateToStudy(StudyRoute.ALL_DUE_DECKS))
            StudyMoreClicked -> studyMore()
        }
    }

    private fun studyMore() {
        productAnalytics.track(ProductEvent.ExtraNewCardsRequested(ExtraNewCardsSource.TODAY))
        sendEffect(NavigateToStudy(StudyRoute.ALL_DUE_DECKS, EXTRA_NEW_CARDS_PER_REQUEST))
    }

    private fun loadStats() {
        viewModelScope.launch {
            val stats: DashboardStats = getDashboardStatsUseCase()
            setState { copy(stats = stats, isLoading = false) }
        }
    }
}
