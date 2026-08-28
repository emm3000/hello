package com.emm.hello.newfeatures.today

import com.emm.hello.core.mvi.MviEffect

sealed interface TodayUiEffect : MviEffect

data class NavigateToStudy(val deckId: String) : TodayUiEffect
