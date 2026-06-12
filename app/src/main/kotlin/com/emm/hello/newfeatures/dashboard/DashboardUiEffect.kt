package com.emm.hello.newfeatures.dashboard

import com.emm.hello.core.mvi.MviEffect

sealed interface DashboardUiEffect : MviEffect

data class NavigateToStudy(val deckId: String) : DashboardUiEffect

data class ShowUndoDeckDeleted(
    val deckName: String,
    val deckId: String,
    val deletedAt: Long,
) : DashboardUiEffect
