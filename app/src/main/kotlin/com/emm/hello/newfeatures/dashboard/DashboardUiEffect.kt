package com.emm.hello.newfeatures.dashboard

import com.emm.hello.core.mvi.MviEffect

sealed interface DashboardUiEffect : MviEffect

data class NavigateToStudy(val deckId: String) : DashboardUiEffect
