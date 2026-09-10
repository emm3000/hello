package com.emm.domain.suggestion

sealed interface SuggestionRefreshStatus {
    data object Idle : SuggestionRefreshStatus
    data object Running : SuggestionRefreshStatus
    data object Completed : SuggestionRefreshStatus
    data object Offline : SuggestionRefreshStatus
    data class Failed(val error: Throwable) : SuggestionRefreshStatus
}
