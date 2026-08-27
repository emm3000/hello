package com.emm.hello.newfeatures.hoy

import com.emm.hello.core.mvi.MviIntent

sealed interface HoyUiIntent : MviIntent

data object StudyClicked : HoyUiIntent

data object ScreenVisible : HoyUiIntent
