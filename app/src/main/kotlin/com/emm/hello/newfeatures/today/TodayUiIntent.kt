package com.emm.hello.newfeatures.today

import com.emm.hello.core.mvi.MviIntent

sealed interface TodayUiIntent : MviIntent

data object StudyClicked : TodayUiIntent

data object ScreenVisible : TodayUiIntent
