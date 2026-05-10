package com.emm.hello.newfeatures.dashboard

sealed interface DashboardUiIntent

data class QueryChanged(val value: String) : DashboardUiIntent

data class TagToggled(val tag: String) : DashboardUiIntent

data object ClearFilters : DashboardUiIntent
