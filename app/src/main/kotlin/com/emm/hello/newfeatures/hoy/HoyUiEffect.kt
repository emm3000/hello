package com.emm.hello.newfeatures.hoy

import com.emm.hello.core.mvi.MviEffect

sealed interface HoyUiEffect : MviEffect

data class NavigateToStudy(val deckId: String) : HoyUiEffect
