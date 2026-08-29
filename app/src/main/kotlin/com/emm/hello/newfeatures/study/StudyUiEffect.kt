package com.emm.hello.newfeatures.study

import com.emm.hello.core.mvi.MviEffect

sealed interface StudyUiEffect : MviEffect {
    data object NavigateBack : StudyUiEffect
    data object NavigateToCapture : StudyUiEffect
    data object NavigateToSuggest : StudyUiEffect
}
