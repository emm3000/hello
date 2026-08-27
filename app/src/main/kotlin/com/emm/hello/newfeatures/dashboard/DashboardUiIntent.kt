package com.emm.hello.newfeatures.dashboard

import com.emm.hello.core.mvi.MviIntent

sealed interface DashboardUiIntent : MviIntent

data object StudyClicked : DashboardUiIntent

data object ScreenVisible : DashboardUiIntent
