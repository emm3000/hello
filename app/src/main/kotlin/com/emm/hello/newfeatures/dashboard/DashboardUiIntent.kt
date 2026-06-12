package com.emm.hello.newfeatures.dashboard

import com.emm.hello.core.mvi.MviIntent

sealed interface DashboardUiIntent : MviIntent

data class QueryChanged(val value: String) : DashboardUiIntent

data class TagToggled(val tag: String) : DashboardUiIntent

data object ClearFilters : DashboardUiIntent

data object StudyClicked : DashboardUiIntent
