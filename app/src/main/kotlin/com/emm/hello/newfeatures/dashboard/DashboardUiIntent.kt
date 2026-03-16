package com.emm.hello.newfeatures.dashboard

sealed interface DashboardUiIntent {
    data object RefreshSync : DashboardUiIntent
}
