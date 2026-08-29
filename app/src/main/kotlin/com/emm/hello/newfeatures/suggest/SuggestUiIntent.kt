package com.emm.hello.newfeatures.suggest

import com.emm.hello.core.mvi.MviIntent

sealed interface SuggestUiIntent : MviIntent {

    data object Retry : SuggestUiIntent
    data class WordToggled(val word: String) : SuggestUiIntent
    data object AddSelected : SuggestUiIntent
    data object BackClicked : SuggestUiIntent
}
