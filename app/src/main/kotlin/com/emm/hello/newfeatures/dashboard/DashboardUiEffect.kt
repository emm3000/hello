package com.emm.hello.newfeatures.dashboard

sealed interface DashboardUiEffect {
    data class SyncFailed(val message: String) : DashboardUiEffect
}
