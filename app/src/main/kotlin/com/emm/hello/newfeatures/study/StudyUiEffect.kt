package com.emm.hello.newfeatures.study

sealed interface StudyUiEffect {
    data object NavigateBack : StudyUiEffect
    data object SessionFinished : StudyUiEffect
}
